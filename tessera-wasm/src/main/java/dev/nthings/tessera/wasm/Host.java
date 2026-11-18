package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.HtmlTag;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.graalvm.webimage.api.JSObject;
import org.graalvm.webimage.api.JSValue;

/// Browser services for one mounted root, bound to a [Lifetime].
///
/// Element operations take an [ElementRef] bound in the root's committed tree and resolve it at
/// call time within the root's host, so application code never holds a native object and cannot
/// reach an element of another root. Resolving a reference that the committed tree does not bind,
/// or using a closed root, fails with an `IllegalStateException`.
///
/// Scheduled callbacks and request completions run only while the lifetime and the root are active,
/// then invalidate the root, like [BrowserRoot#update(Runnable)]; [#afterRender(Runnable)] tasks do
/// not invalidate it again. Disposing the lifetime cancels every pending callback and aborts every
/// request it started; closing the root disposes the root lifetime and every effect lifetime. Use
/// [#within(Lifetime)] to scope work to an effect.
public final class Host {
  private final BrowserRoot root;
  private final Lifetime lifetime;

  /// The root host tracks every pending task; scoped hosts also track their own subset.
  private final Host owner;

  private final Set<Task> active = new LinkedHashSet<>();

  /// On the root host: the scoped host of each lifetime, until that lifetime is disposed.
  private final Map<Lifetime, Host> scoped = new IdentityHashMap<>();

  private boolean registered;

  Host(BrowserRoot root, Lifetime lifetime) {
    this.root = root;
    this.lifetime = lifetime;
    this.owner = this;
  }

  private Host(Host parent, Lifetime lifetime) {
    this.root = parent.root;
    this.lifetime = lifetime;
    this.owner = parent.owner;
  }

  /// Services whose scheduled work and requests are also cancelled when `scope` is disposed. Every
  /// call for one `scope` returns the same services, so repeated calls register nothing more with
  /// it.
  public Host within(Lifetime scope) {
    var hosts = owner.scoped;
    var cached = hosts.get(scope);
    if (cached != null) {
      return cached;
    }
    var host = new Host(this, scope);
    if (!scope.disposed()) {
      hosts.put(scope, host);
      host.registered = true;
      scope.onDispose(() -> {
        hosts.remove(scope);
        host.cancelAll();
      });
    }
    return host;
  }

  // --- Scheduling ---------------------------------------------------------------------------

  /// Runs `task` once after `delay`.
  public Cancellable timeout(Duration delay, Runnable task) {
    var millis = millis(delay);
    return start(handle -> releasing(Dom.timeout(millis, () -> handle.complete(task))));
  }

  /// Runs `task` every `period` until cancelled or disposed.
  public Cancellable interval(Duration period, Runnable task) {
    var millis = millis(period);
    return start(handle -> releasing(Dom.interval(millis, () -> handle.run(task))));
  }

  /// Runs `task` once before the browser's next paint.
  public Cancellable frame(Runnable task) {
    return start(handle -> releasing(Dom.frame(() -> handle.complete(task))));
  }

  /// Runs `task` once after the next committed render, when the committed tree contains the
  /// elements that render placed, for example to focus an element it created. Requests that render;
  /// does not invalidate the root again afterwards.
  public Cancellable afterRender(Runnable task) {
    return start(handle -> root.afterRender(() -> handle.complete(task, false)));
  }

  private static int millis(Duration duration) {
    if (duration.isNegative()) {
      throw new IllegalArgumentException("Duration must not be negative: " + duration);
    }
    return Math.toIntExact(duration.toMillis());
  }

  // --- Requests -----------------------------------------------------------------------------

  /// Fetches `url` as text with `GET`. Exactly one callback runs while the request is active: a
  /// successful status calls `completed` with the body, any other status or a network failure calls
  /// `failed`. Cancelling, disposing the lifetime or closing the root aborts the request.
  public Cancellable fetchText(
      String url, Consumer<String> completed, Consumer<RequestFailure> failed) {
    return start(handle ->
        releasing(
            Dom.fetchText(
                url,
                body -> handle.complete(() -> completed.accept(text(body))),
                (status, message) ->
                    handle.complete(() ->
                        failed.accept(
                            new RequestFailure(
                                JSValue.checkedCoerce(status, Integer.class), text(message)))))));
  }

  private static String text(Object value) {
    return JSValue.checkedCoerce(value, String.class);
  }

  // --- Focus and selection ------------------------------------------------------------------

  public void focus(ElementRef element) {
    Dom.focus(root.resolve(element));
  }

  public boolean focused(ElementRef element) {
    return Dom.isActiveElement(root.resolve(element));
  }

  /// Remembers the focused element, wherever it is, for [FocusReturn#restore()].
  public FocusReturn rememberFocus() {
    root.requireOpen();
    return new FocusReturn(this, Dom.activeElement());
  }

  public TextSelection selection(ElementRef control) {
    var node = textControl(control);
    return new TextSelection(
        Dom.selectionStart(node), Dom.selectionEnd(node), Dom.selectionDirection(node));
  }

  /// Selects characters `start` (inclusive) to `end` (exclusive) of a text input or textarea.
  public void select(ElementRef control, int start, int end) {
    Dom.setSelection(textControl(control), start, end);
  }

  public void selectAll(ElementRef control) {
    Dom.selectAll(textControl(control));
  }

  private JSObject textControl(ElementRef control) {
    var node = root.resolve(control);
    if (!Dom.hasSelection(node)) {
      throw new IllegalArgumentException(control + " is not a text input or textarea");
    }
    return node;
  }

  // --- Dialogs and popovers -----------------------------------------------------------------

  /// Opens a `<dialog>` modally; a no-op when it already is. Call it from a user event handler so
  /// that focus moves into the dialog within the user's activation.
  public void showModal(ElementRef dialog) {
    var node = kind(dialog, "a <dialog>", HtmlTag.DIALOG);
    if (Dom.isModal(node)) {
      return;
    }
    if (Dom.flag(node, "open")) {
      throw new IllegalStateException(dialog + " is open without being modal; close it first");
    }
    Dom.showModal(node);
  }

  /// Opens a `<dialog>` without making the rest of the page inert.
  public void show(ElementRef dialog) {
    var node = kind(dialog, "a <dialog>", HtmlTag.DIALOG);
    if (!Dom.flag(node, "open")) {
      Dom.show(node);
    }
  }

  /// Closes a `<dialog>`; a no-op when it is closed. The browser fires its `close` event.
  public void close(ElementRef dialog) {
    close(dialog, "");
  }

  public void close(ElementRef dialog, String returnValue) {
    var node = kind(dialog, "a <dialog>", HtmlTag.DIALOG);
    if (Dom.flag(node, "open")) {
      Dom.closeDialog(node, returnValue);
    }
  }

  public boolean open(ElementRef dialogOrPopover) {
    var node = root.resolve(dialogOrPopover);
    return Dom.flag(node, "open") || Dom.popoverOpen(node);
  }

  public void showPopover(ElementRef popover) {
    var node = popover(popover);
    if (!Dom.popoverOpen(node)) {
      Dom.showPopover(node);
    }
  }

  public void hidePopover(ElementRef popover) {
    var node = popover(popover);
    if (Dom.popoverOpen(node)) {
      Dom.hidePopover(node);
    }
  }

  /// Toggles the popover bound to `popover` and returns whether it is open afterwards.
  public boolean togglePopover(ElementRef popover) {
    return Dom.togglePopover(popover(popover));
  }

  private JSObject popover(ElementRef popover) {
    var node = root.resolve(popover);
    if (!Dom.hasPopover(node)) {
      throw new IllegalArgumentException(popover + " has no popover attribute");
    }
    return node;
  }

  // --- Media --------------------------------------------------------------------------------

  /// Starts playback of an `<audio>` or `<video>`. `playing` runs once playback has started;
  /// `failed` receives the browser's reason, for example an autoplay policy rejection.
  public void play(ElementRef media, Runnable playing, Consumer<String> failed) {
    var node = mediaElement(media);
    start(handle -> {
      Dom.play(
          node,
          () -> handle.complete(playing),
          reason -> handle.complete(() -> failed.accept(text(reason))));
      // A play request cannot be withdrawn; cancelling only drops its callbacks.
      return () -> {};
    });
  }

  public void pause(ElementRef media) {
    Dom.pause(mediaElement(media));
  }

  public void seek(ElementRef media, double seconds) {
    Dom.seek(mediaElement(media), seconds);
  }

  public void muted(ElementRef media, boolean muted) {
    Dom.mute(mediaElement(media), muted);
  }

  public MediaState media(ElementRef media) {
    var node = mediaElement(media);
    return new MediaState(
        Dom.flag(node, "paused"),
        Dom.flag(node, "ended"),
        Dom.flag(node, "muted"),
        Dom.number(node, "currentTime"),
        Dom.number(node, "duration"),
        Dom.number(node, "volume"));
  }

  private JSObject mediaElement(ElementRef media) {
    return kind(media, "an <audio> or <video>", HtmlTag.AUDIO, HtmlTag.VIDEO);
  }

  /// The committed element bound to `ref`, which must have one of `tags`.
  private JSObject kind(ElementRef ref, String description, HtmlTag... tags) {
    var node = root.resolve(ref);
    var tagName = Dom.tagName(node);
    if (Arrays.stream(tags).noneMatch(tag -> tag.tagName().equalsIgnoreCase(tagName))) {
      throw new IllegalArgumentException(ref + " is not " + description);
    }
    return node;
  }

  // --- Ownership ----------------------------------------------------------------------------

  /// Starts work only while this host is alive; `work` begins the native operation and returns its
  /// cancellation. The first started work registers cancellation with the lifetime.
  private Cancellable start(Function<Task, Runnable> work) {
    var task = new Task();
    if (!alive()) {
      return task;
    }
    // A scoped host registered with its lifetime when it was created.
    owner.register();
    task.cleanup = work.apply(task);
    active.add(task);
    owner.active.add(task);
    return task;
  }

  private void register() {
    if (!registered) {
      registered = true;
      lifetime.onDispose(this::cancelAll);
    }
  }

  private static Runnable releasing(JSObject cleanup) {
    return () -> Dom.release(cleanup);
  }

  private void cancelAll() {
    for (var task : new ArrayList<>(active)) {
      task.cancel();
    }
  }

  boolean alive() {
    return !lifetime.disposed() && !root.closed();
  }

  private static final Runnable NO_CLEANUP = () -> {};

  /// One scheduled callback or request. It is pending while this host holds it.
  private final class Task implements Cancellable {
    // Dropped once the task ends, so a retained handle does not keep the native work alive.
    private Runnable cleanup = NO_CLEANUP;

    /// Runs a repeating callback.
    void run(Runnable callback) {
      if (active.contains(this) && alive()) {
        root.runScheduled(callback, true);
      }
    }

    /// Runs the final callback of one-shot work, then re-renders the root.
    void complete(Runnable callback) {
      complete(callback, true);
    }

    void complete(Runnable callback, boolean invalidate) {
      if (active.remove(this)) {
        owner.active.remove(this);
        cleanup = NO_CLEANUP;
        if (alive()) {
          root.runScheduled(callback, invalidate);
        }
      }
    }

    @Override
    public void cancel() {
      if (active.remove(this)) {
        owner.active.remove(this);
        var ending = cleanup;
        cleanup = NO_CLEANUP;
        ending.run();
      }
    }
  }
}
