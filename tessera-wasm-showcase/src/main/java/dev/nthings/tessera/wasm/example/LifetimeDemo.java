package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.Lifetime;
import dev.nthings.tessera.wasm.BrowserRoot;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Lifetime evidence: keyed effects, root cleanup, requests and transient UI on close.
///
/// The control root outlives the lifetime root, so it can show what disposal did.
public final class LifetimeDemo {
  private final List<String> log = new ArrayList<>();
  private @Nullable BrowserRoot control;
  private final MountedRoot lifetimeRoot = new MountedRoot("lifetime view");
  private String host = "lifetime";
  private final ElementRef dialog = ElementRef.scoped("dialog");
  private int ticks;
  private boolean timer;
  private int generation;
  private String request = "idle";

  /// Mounts the lifetime view; also used to remount it after it closes.
  public BrowserRoot mount(String host) {
    var mounted = BrowserRoot.mount(host, this::view);
    mounted.lifetime().onDispose(() -> record("root disposed"));
    this.host = host;
    lifetimeRoot.set(mounted);
    return mounted;
  }

  public BrowserRoot mountControl(String host) {
    var mounted = BrowserRoot.mount(host, this::control);
    control = mounted;
    return mounted;
  }

  private IntoElement view(BrowserRoot root, DocumentContext context) {
    var current = generation;
    return Elements.section()
        .attr("data-lifetime-ticks", Integer.toString(ticks))
        .children(
            Elements.h2().text("Lifetime"),
            timer
                ? Elements.effect("ticker-" + current, lifetime -> tick(lifetime, current))
                : Elements.empty(),
            Button.of("Start timer").onClick(root.update(() -> timer = true)),
            Button.of("Restart timer").onClick(root.update(() -> generation++)),
            Button.of("Stop timer").onClick(root.update(() -> timer = false)),
            Button.of("Start request").onClick(this::startRequest),
            Button.of("Fetch text").onClick(() -> fetch("/text")),
            Button.of("Fetch missing").onClick(() -> fetch("/missing")),
            Button.of("Open lifetime dialog").onClick(() -> root.host().showModal(dialog)),
            Elements.dialog()
                .id(dialog)
                .ariaLabel("Lifetime dialog")
                .children(
                    Elements.p().text("Modal inside the lifetime root"),
                    Button.of("Close from dialog").onClick(root::close)),
            Elements.p().attr("data-lifetime-status", "").text("ticks=" + ticks));
  }

  private void tick(Lifetime lifetime, int current) {
    record("start " + current);
    lifetimeRoot.get().host().within(lifetime).interval(Duration.ofMillis(20), () -> ticks++);
    lifetime.onDispose(() -> record("stop " + current));
  }

  /// `/slow` never completes before the root closes; closing aborts it and no callback runs.
  private void startRequest() {
    setRequest("pending");
    fetch("/slow");
  }

  private void fetch(String url) {
    lifetimeRoot
        .get()
        .host()
        .fetchText(
            url,
            body -> setRequest("text " + body),
            failure -> setRequest("http " + failure.status()));
  }

  private IntoElement control(BrowserRoot control, DocumentContext context) {
    var entries = String.join(",", log);
    return Elements.section()
        .attr("data-lifetime-log", entries)
        .attr("data-lifetime-request", request)
        .children(
            Elements.h2().text("Lifetime control"),
            Button.of("Close lifetime root").onClick(() -> lifetimeRoot.get().close()),
            Button.of("Remount lifetime root").onClick(() -> mount(host)),
            Button.of("Mount lifetime twice").onClick(() -> BrowserRoot.mount(host, this::view)),
            Elements.p().text("log=" + entries + "; request=" + request));
  }

  private void record(String entry) {
    log.add(entry);
    refreshControl();
  }

  private void setRequest(String status) {
    request = status;
    refreshControl();
  }

  private void refreshControl() {
    var mounted = control;
    if (mounted != null) {
      mounted.invalidate();
    }
  }
}
