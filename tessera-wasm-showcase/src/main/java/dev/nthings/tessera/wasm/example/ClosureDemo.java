package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.Live;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Commit-ownership evidence: what each commit phase owns, where closure settles, and what stays
/// callable afterwards. The control root outlives the root under test, so its log shows what
/// disposal did after that root closed.
public final class ClosureDemo {
  private final List<String> log = new ArrayList<>();
  private @Nullable BrowserRoot controlRoot;
  private boolean guard = true;
  private int action = 1;
  private boolean closeWhileRendering;
  private boolean startupBatch;
  private boolean failStart;
  private boolean failingCleanups;

  /// Mounts the root under test, recording its root-wide disposal in the log.
  public BrowserRoot mount(String host) {
    var mounted = BrowserRoot.mount(host, this::view);
    mounted.lifetime().onDispose(() -> record("root disposed"));
    return mounted;
  }

  public BrowserRoot mountControl(String host) {
    var mounted = BrowserRoot.mount(host, this::control);
    controlRoot = mounted;
    return mounted;
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    if (closeWhileRendering) {
      // Closing during expansion: the candidate this view returns must never publish.
      root.close();
    }
    var children = new ArrayList<LiveContent>();
    children.add(LiveContent.of(Elements.h2().text("Closure")));
    if (closeWhileRendering) {
      children.add(
          LiveContent.of(Elements.p().attr("data-never-published", "").text("Never published")));
    }
    // Removing this effect retires it after publication, and its cleanup closes the root
    // mid-retire while the commit still owns what it acquired.
    if (guard) {
      children.add(Live.effect("guard", lifetime -> lifetime.onDispose(root::close)));
    }
    if (startupBatch) {
      children.add(
          Live.effect(
              "closing-start",
              lifetime -> {
                record("started closing-start");
                lifetime.onDispose(() -> record("disposed closing-start"));
                root.close();
              }));
      children.add(Live.effect("observing-start", lifetime -> record("started observing-start")));
    }
    if (failStart) {
      children.add(
          Live.effect(
              "failing",
              lifetime -> {
                lifetime.onDispose(() -> record("disposed failing"));
                throw new IllegalStateException("Simulated effect start failure");
              }));
    }
    if (failingCleanups) {
      children.add(
          Live.effect(
              "victim",
              lifetime ->
                  lifetime.onDispose(() -> {
                    record("disposed victim");
                    throw new IllegalStateException("Simulated cleanup failure");
                  })));
      children.add(
          Live.effect(
              "survivor", lifetime -> lifetime.onDispose(() -> record("disposed survivor"))));
    }
    // Keyed, so replacing it creates a new node the same commit owns until publication.
    children.add(
        Live.fragment(Ui.button("Action " + action, () -> record("clicked action " + action)))
            .key("action-" + action));
    children.add(
        Ui.button(
            "Replace action",
            root.update(() -> {
              guard = false;
              action++;
            })));
    children.add(Ui.button("Close during render", root.update(() -> closeWhileRendering = true)));
    children.add(Ui.button("Close during startup", root.update(() -> startupBatch = true)));
    children.add(
        Ui.button(
            "Close in after-render",
            () -> {
              root.host()
                  .afterRender(() -> {
                    record("after-render first");
                    root.close();
                  });
              root.host().afterRender(() -> record("after-render second"));
            }));
    children.add(Ui.button("Fail effect start", root.update(() -> failStart = true)));
    children.add(Ui.button("Add failing cleanups", root.update(() -> failingCleanups = true)));
    children.add(Ui.button("Remove failing cleanups", root.update(() -> failingCleanups = false)));
    children.add(Ui.button("Close closure root", root::close));
    return LiveElement.of(Elements.section().attr("data-closure", "")).children(children);
  }

  private LiveContent control(LiveContext context) {
    var entries = String.join(",", log);
    return LiveContent.of(
        Elements.section()
            .attr("data-closure-log", entries)
            .children(Elements.h2().text("Closure control"), Elements.p().text("log=" + entries)));
  }

  private void record(String entry) {
    log.add(entry);
    var control = controlRoot;
    if (control != null) {
      control.invalidate();
    }
  }
}
