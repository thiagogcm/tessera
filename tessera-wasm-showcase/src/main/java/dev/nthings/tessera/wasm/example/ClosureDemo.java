package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.wasm.BrowserRoot;
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

  private IntoElement view(BrowserRoot root, DocumentContext context) {
    if (closeWhileRendering) {
      // Closing during expansion: the candidate this view returns must never publish.
      root.close();
    }
    var children = new ArrayList<IntoElement>();
    children.add(Elements.h2().text("Closure"));
    if (closeWhileRendering) {
      children.add(Elements.p().attr("data-never-published", "").text("Never published"));
    }
    // Removing this effect retires it after publication, and its cleanup closes the root
    // mid-retire while the commit still owns what it acquired.
    if (guard) {
      children.add(Elements.effect("guard", lifetime -> lifetime.onDispose(root::close)));
    }
    if (startupBatch) {
      children.add(
          Elements.effect(
              "closing-start",
              lifetime -> {
                record("started closing-start");
                lifetime.onDispose(() -> record("disposed closing-start"));
                root.close();
              }));
      children.add(
          Elements.effect("observing-start", lifetime -> record("started observing-start")));
    }
    if (failStart) {
      children.add(
          Elements.effect(
              "failing",
              lifetime -> {
                lifetime.onDispose(() -> record("disposed failing"));
                throw new IllegalStateException("Simulated effect start failure");
              }));
    }
    if (failingCleanups) {
      children.add(
          Elements.effect(
              "victim",
              lifetime ->
                  lifetime.onDispose(() -> {
                    record("disposed victim");
                    throw new IllegalStateException("Simulated cleanup failure");
                  })));
      children.add(
          Elements.effect(
              "survivor", lifetime -> lifetime.onDispose(() -> record("disposed survivor"))));
    }
    // Keyed, so replacing it creates a new node the same commit owns until publication.
    children.add(
        Elements.fragment()
            .children(
                Button.of("Action " + action).onClick(() -> record("clicked action " + action)))
            .key("action-" + action));
    children.add(
        Button.of("Replace action")
            .onClick(
                root.update(() -> {
                  guard = false;
                  action++;
                })));
    children.add(
        Button.of("Close during render").onClick(root.update(() -> closeWhileRendering = true)));
    children.add(Button.of("Close during startup").onClick(root.update(() -> startupBatch = true)));
    children.add(
        Button.of("Close in after-render")
            .onClick(() -> {
              root.host()
                  .afterRender(() -> {
                    record("after-render first");
                    root.close();
                  });
              root.host().afterRender(() -> record("after-render second"));
            }));
    children.add(Button.of("Fail effect start").onClick(root.update(() -> failStart = true)));
    children.add(
        Button.of("Add failing cleanups").onClick(root.update(() -> failingCleanups = true)));
    children.add(
        Button.of("Remove failing cleanups").onClick(root.update(() -> failingCleanups = false)));
    children.add(Button.of("Close closure root").onClick(root::close));
    return Elements.section().attr("data-closure", "").children(children);
  }

  private IntoElement control(BrowserRoot control, DocumentContext context) {
    var entries = String.join(",", log);
    return Elements.section()
        .attr("data-closure-log", entries)
        .children(Elements.h2().text("Closure control"), Elements.p().text("log=" + entries));
  }

  private void record(String entry) {
    log.add(entry);
    var control = controlRoot;
    if (control != null) {
      control.invalidate();
    }
  }
}
