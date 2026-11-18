package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.Lifetime;
import dev.nthings.tessera.wasm.Live;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
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

  private LiveContent view(LiveContext context) {
    var root = context.root();
    var current = generation;
    return LiveElement.of(
            Elements.section()
                .attr("data-lifetime-ticks", Integer.toString(ticks))
                .child(Elements.h2().text("Lifetime")))
        .children(
            timer
                ? Live.effect("ticker-" + current, lifetime -> tick(lifetime, current))
                : Live.fragment(),
            Ui.button("Start timer", root.update(() -> timer = true)),
            Ui.button("Restart timer", root.update(() -> generation++)),
            Ui.button("Stop timer", root.update(() -> timer = false)),
            Ui.button("Start request", this::startRequest),
            Ui.button("Fetch text", () -> fetch("/text")),
            Ui.button("Fetch missing", () -> fetch("/missing")),
            Ui.button("Open lifetime dialog", () -> root.host().showModal(dialog)),
            LiveElement.of(
                    Elements.dialog()
                        .id(dialog)
                        .ariaLabel("Lifetime dialog")
                        .child(Elements.p().text("Modal inside the lifetime root")))
                .child(Ui.button("Close from dialog", root::close)))
        .child(Elements.p().attr("data-lifetime-status", "").text("ticks=" + ticks));
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

  private LiveContent control(LiveContext context) {
    var entries = String.join(",", log);
    return LiveElement.of(
            Elements.section()
                .attr("data-lifetime-log", entries)
                .attr("data-lifetime-request", request)
                .child(Elements.h2().text("Lifetime control")))
        .children(
            Ui.button("Close lifetime root", () -> lifetimeRoot.get().close()),
            Ui.button("Remount lifetime root", () -> mount(host)),
            Ui.button("Mount lifetime twice", () -> BrowserRoot.mount(host, this::view)))
        .child(Elements.p().text("log=" + entries + "; request=" + request));
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
