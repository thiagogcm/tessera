package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.ViewException;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.view.EmbeddedStyles;
import dev.nthings.tessera.view.ViewRenderer;
import dev.nthings.tessera.view.ViewSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;

/// Experimental native DOM mount for shared Java components.
///
/// Invalidation reconciles compatible native nodes by sibling key, or by kind among unkeyed
/// siblings (tag name, or component type for a component's DOM range). Each flush renders a bounded
/// number of turns, coalesces pending invalidations and yields a paint opportunity between batches.
/// Repeated self-invalidation stops with a reported diagnostic.
///
/// Generated and [dev.nthings.tessera.core.ElementRef#scoped] ids are prefixed with the host id, so
/// roots on one page never collide. The host's own id is reserved: a child claiming it rejects the
/// render. An id the render introduces, one the root's last commit did not render, that another
/// root or the host page already uses rejects the render (`cross-root-id`); ids the root has
/// committed are its own and are not checked again.
///
/// The root owns its host element: it marks it for embedded styles, replaces fallback content on
/// the first successful render and empties it on [#close()]. Style rules are shared with other
/// roots through one document stylesheet and released when no committed tree uses them.
public final class BrowserRoot implements AutoCloseable {

  /// Scheduling limits for one mounted root: `workBudget` render turns run in one browser task
  /// before yielding to paint, and after `runawayLimit` consecutive turns that each request another
  /// render, the root stops and reports a runaway. Both must be positive, or the constructor throws
  /// IllegalArgumentException.
  public record Scheduling(int workBudget, int runawayLimit) {
    /// Eight turns per task, runaway after 32 self-requested turns.
    public static final Scheduling DEFAULT = new Scheduling(8, 32);

    public Scheduling {
      if (workBudget < 1) {
        throw new IllegalArgumentException("workBudget must be positive");
      }
      if (runawayLimit < 1) {
        throw new IllegalArgumentException("runawayLimit must be positive");
      }
    }
  }

  private final DomReconciler renderer;
  private final JSObject hostElement;
  private @Nullable LiveComponent view;
  private final LifetimeScope lifetime = new LifetimeScope();
  private final Host services;
  private Map<ElementRef, String> committedReferences = Map.of();

  /// The DOM ids of the committed tree, which the root owns; only other ids are checked.
  private Set<String> committedIds = Set.of();

  private final List<Runnable> afterRender = new ArrayList<>();
  private final Theme theme = Theme.defaults();
  private final StyleSink.Root styles = StyleSink.shared().root(theme);
  private final String hostId;
  private final Scheduling scheduling;
  private @Nullable JSObject pending;
  private boolean dirty;
  private boolean rendering;
  private int selfInvalidations;
  private boolean closed;

  private BrowserRoot(String hostId, Scheduling scheduling, LiveComponent view) {
    this.hostId = hostId;
    this.scheduling = scheduling;
    this.hostElement = Dom.host(hostId);
    this.renderer = new DomReconciler(hostElement, hostId, this::invalidate, () -> !closed);
    this.view = view;
    this.services = new Host(this, lifetime);
  }

  /// Mounts with the default scheduling limits.
  ///
  /// The view receives a [LiveContext] whose [LiveContext#root()] is this root.
  public static BrowserRoot mount(String hostId, LiveComponent view) {
    return mount(hostId, Scheduling.DEFAULT, view);
  }

  /// The view receives a [LiveContext] whose [LiveContext#root()] is this root, and returns live
  /// content; wrap portable content with [LiveContent#of].
  ///
  /// The first render runs synchronously. A rejected candidate keeps the host's fallback content,
  /// and a later invalidation retries. Other roots mount independently.
  public static BrowserRoot mount(String hostId, Scheduling scheduling, LiveComponent view) {
    var root = new BrowserRoot(hostId, scheduling, view);
    var conflict = Dom.mountConflict(root.hostElement, EmbeddedStyles.ROOT_ATTRIBUTE);
    if (conflict != null) {
      throw new IllegalStateException(
          "Cannot mount a browser root on '"
              + hostId
              + "': the host "
              + conflict
              + ". Close that root first, or mount on a separate host outside every root.");
    }
    Dom.attribute(root.hostElement, EmbeddedStyles.ROOT_ATTRIBUTE, "");
    Dom.attribute(
        root.hostElement, EmbeddedStyles.THEME_ATTRIBUTE, EmbeddedStyles.themeKey(root.theme));
    root.styles.mount();
    root.turn();
    if (root.dirty && !root.closed) {
      root.pending = Dom.defer(root::flush);
    }
    return root;
  }

  /// Wraps `change` so that running it applies the change and invalidates this root.
  ///
  /// After [#close()] the wrapper does nothing. Asynchronous work wraps its completion when it
  /// starts, so a late result cannot mutate a disposed root.
  public Runnable update(Runnable change) {
    return () -> {
      if (closed) {
        return;
      }
      change.run();
      invalidate();
    };
  }

  /// Cleanups registered here run when the root closes, after its effects are disposed.
  public Lifetime lifetime() {
    return lifetime;
  }

  /// Browser services bound to this root's lifetime.
  public Host host() {
    return services;
  }

  boolean closed() {
    return closed;
  }

  void requireOpen() {
    if (closed) {
      throw new IllegalStateException("Browser root '" + hostId + "' is closed");
    }
  }

  /// The committed element bound to `ref`, found within this root's host.
  JSObject resolve(ElementRef ref) {
    requireOpen();
    var id = committedReferences.get(ref);
    var node = id == null ? null : Dom.find(hostElement, id);
    if (node == null) {
      throw new IllegalStateException(
          ref
              + " is not bound in the committed tree of browser root '"
              + hostId
              + "'. References resolve against the latest committed render: keep a reference"
              + " that effects, timers or request callbacks use in application state, and pass"
              + " it to the component that binds it on every render.");
    }
    return node;
  }

  /// Queues `task` for after the next committed render and requests that render; returns the
  /// removal of the queued task.
  Runnable afterRender(Runnable task) {
    afterRender.add(task);
    invalidate();
    return () -> afterRender.remove(task);
  }

  /// Runs a scheduled task outside a render, reporting its failure, then optionally invalidates.
  void runScheduled(Runnable work, boolean invalidate) {
    Failures.run("Scheduled task", hostId, work);
    if (invalidate) {
      invalidate();
    }
  }

  /// Coalesces requests into the current flush and leaves event handlers synchronous.
  public void invalidate() {
    if (closed) {
      return;
    }
    dirty = true;
    if (rendering) {
      return;
    }
    selfInvalidations = 0;
    if (pending == null) {
      pending = Dom.defer(this::flush);
    }
  }

  private void flush() {
    pending = null;
    var turns = 0;
    while (dirty && !closed && turns < scheduling.workBudget()) {
      dirty = false;
      turns++;
      turn();
      // Only the turn itself can set `dirty` while it renders.
      if (dirty) {
        selfInvalidations++;
        if (selfInvalidations >= scheduling.runawayLimit()) {
          dirty = false;
          Dom.reportError(runawayMessage());
        }
      } else {
        selfInvalidations = 0;
      }
    }
    if (dirty && !closed) {
      pending = Dom.paint(this::flush);
    }
  }

  /// Expands a candidate, then applies, publishes, retires and activates it in phases.
  ///
  /// A rejected candidate or failed render callback leaves the committed tree unchanged and permits
  /// another invalidation. The candidate publishes at one boundary: its tree, references and IDs
  /// install together, obsolete resources release with their cleanups, and new effects start only
  /// while the root remains open. Closure requested at any boundary settles there: nothing
  /// publishes or starts afterwards. An unexpected native failure closes the root and is reported;
  /// a failure before the first publication leaves the fallback content in the host.
  private void turn() {
    rendering = true;
    try {
      var content = view;
      if (content == null) {
        return;
      }
      ViewSnapshot<LiveBehavior> snapshot;
      try {
        snapshot =
            ViewRenderer.render(
                context -> LivePlacement.capture(content.expand((LiveContext) context)),
                hostId,
                base -> new RootContext(base, this),
                LiveBehavior::validate);
      } catch (RuntimeException rejected) {
        reject(
            rejected instanceof ViewException
                ? String.valueOf(rejected.getMessage())
                : Failures.describe(rejected));
        return;
      }
      // Closing during expansion discards the result: nothing publishes afterwards.
      if (closed) {
        return;
      }
      var foreign = foreignId(snapshot.ids());
      if (foreign != null) {
        var owner = foreign.split("\n", 2);
        reject(
            new Diagnostic(
                    "cross-root-id",
                    "DOM id '" + owner[0] + "' is already used by " + owner[1],
                    "Give the element an id that is unique in the page, or bind a scoped"
                        + " ElementRef so the id is prefixed with this root's host id.",
                    Location.ROOT)
                .format());
        return;
      }
      try {
        styles.update(
            snapshot.styleRules(),
            () -> {
              var commit = renderer.apply(snapshot);
              renderer.publish(commit);
              committedReferences = snapshot.references();
              committedIds = snapshot.ids();
            });
        renderer.retire();
        renderer.activate();
        runAfterRender();
      } catch (ClosedRoot _) {
        return;
      } catch (RuntimeException hostFailure) {
        close();
        Failures.report("DOM commit", hostId, hostFailure);
        return;
      }
    } finally {
      rendering = false;
    }
  }

  /// Runs queued after-render work while the root stays open. A task that closes the root stops the
  /// rest of the batch, and a failing task is reported without affecting the root.
  private void runAfterRender() {
    while (!closed && !afterRender.isEmpty()) {
      var task = afterRender.remove(0);
      Failures.run("After-render task", hostId, task);
    }
  }

  /// The first of `ids` an element outside the root's committed tree already uses, followed by a
  /// newline and that element's owner, or `null`. The host's own id is reserved, so a child
  /// claiming it is foreign. Committed ids are the root's own, so markup the host page adds later
  /// with one of them does not block renders; an id the root removed and renders again is checked
  /// again.
  private @Nullable String foreignId(Set<String> ids) {
    if (ids.contains(hostId)) {
      return hostId + "\n" + "the host element of this root";
    }
    var introduced = new StringJoiner("\n");
    for (var id : ids) {
      if (!committedIds.contains(id)) {
        introduced.add(id);
      }
    }
    return introduced.length() == 0
        ? null
        : Dom.foreignId(hostElement, introduced.toString(), EmbeddedStyles.ROOT_ATTRIBUTE);
  }

  /// Rejects the candidate: the committed tree is unchanged and a later invalidation retries.
  private void reject(String details) {
    dirty = false;
    Dom.reportError(
        Failures.inRoot("Render rejected", hostId)
            + "; the committed view is unchanged:\n"
            + details);
  }

  private void cancelPending() {
    var scheduled = pending;
    pending = null;
    if (scheduled != null) {
      Dom.release(scheduled);
    }
  }

  private String runawayMessage() {
    return Failures.inRoot("Runaway invalidation", hostId)
        + ": "
        + selfInvalidations
        + " consecutive render turns requested another render";
  }

  /// Disposes the root. Idempotent; safe to call from the root's own handlers and effects.
  ///
  /// In order: cancels scheduled work, releases listeners, disposes effects (innermost first),
  /// closes dialogs and popovers, empties the host, and disposes the root [#lifetime()]. Disposal
  /// cancels tasks and requests and releases styles. The view callback is dropped. Application
  /// cleanup failures are collected and reported together once.
  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    dirty = false;
    view = null;
    var failures = new ArrayList<RuntimeException>();
    cancelPending();
    committedReferences = Map.of();
    committedIds = Set.of();
    afterRender.clear();
    renderer.close(failures);
    lifetime.dispose(failures);
    styles.close();
    Dom.removeAttribute(hostElement, EmbeddedStyles.ROOT_ATTRIBUTE);
    Dom.removeAttribute(hostElement, EmbeddedStyles.THEME_ATTRIBUTE);
    Failures.reportAll("Cleanup failed while closing browser root '" + hostId + "'", failures);
  }
}
