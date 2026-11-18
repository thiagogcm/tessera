package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.Lifetime;
import dev.nthings.tessera.wasm.Live;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import java.time.Duration;

/// A panel with an auto-refresh timer, a slow report request and a cancellable preview, plus a
/// toolbar root that reopens the panel after it closes. What the panel starts and stops is listed
/// in the shared [ActivityLog].
final class LifetimeDemo {
  private final ActivityLog activity;
  private final MountedRoot panel = new MountedRoot("lifetime panel");
  private final ElementRef settings = ElementRef.scoped("settings");
  private String host = "lifetime";
  private int ticks;
  private boolean refreshing;
  private int generation;
  private boolean preview;

  LifetimeDemo(ActivityLog activity) {
    this.activity = activity;
  }

  /// Mounts the panel; the toolbar also uses it to reopen the panel after it closes.
  BrowserRoot mount(String host) {
    var mounted = BrowserRoot.mount(host, this::view);
    mounted.lifetime().onDispose(() -> activity.record("panel closed"));
    this.host = host;
    panel.set(mounted);
    return mounted;
  }

  /// Mounts the toolbar, which outlives the panel.
  BrowserRoot mountToolbar(String host) {
    return BrowserRoot.mount(
        host,
        _ ->
            LiveElement.of(Elements.nav().ariaLabel("Panel toolbar"))
                .children(
                    Ui.button("Reopen panel", () -> mount(this.host)),
                    Ui.button("Open panel again", () -> BrowserRoot.mount(this.host, this::view))));
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    var current = generation;
    return LiveElement.of(
            Elements.section()
                .ariaLabel("Panel")
                .children(
                    Elements.h2().text("Panel"),
                    Elements.p().role(Role.STATUS).text("Refreshed " + ticks + " times")))
        .children(
            refreshing
                ? Live.effect("refresh-" + current, lifetime -> refresh(lifetime, current))
                : Live.fragment(),
            preview ? Live.effect("preview", this::loadPreview) : Live.fragment(),
            Ui.button("Start auto-refresh", root.update(() -> refreshing = true)),
            Ui.button("Restart auto-refresh", root.update(() -> generation++)),
            Ui.button("Stop auto-refresh", root.update(() -> refreshing = false)),
            Ui.button("Load preview", root.update(() -> preview = true)),
            Ui.button("Cancel preview", root.update(() -> preview = false)),
            Ui.button("Load report", this::loadReport),
            Ui.button("Open settings", () -> root.host().showModal(settings)),
            LiveElement.of(
                    Elements.dialog()
                        .id(settings)
                        .ariaLabel("Panel settings")
                        .child(Elements.p().text("Settings for this panel")))
                .child(Ui.button("Close panel", root::close)),
            Ui.button("Close", root::close));
  }

  private void refresh(Lifetime lifetime, int current) {
    activity.record("auto-refresh " + current + " started");
    panel.get().host().within(lifetime).interval(Duration.ofSeconds(1), () -> ticks++);
    lifetime.onDispose(() -> activity.record("auto-refresh " + current + " stopped"));
  }

  /// The preview request belongs to its effect: removing the effect aborts it.
  private void loadPreview(Lifetime lifetime) {
    panel
        .get()
        .host()
        .within(lifetime)
        .fetchText(
            "/slow",
            _ -> activity.record("preview loaded"),
            failure -> activity.record("preview failed " + failure.status()));
  }

  /// The report request belongs to the panel: closing the panel aborts it.
  private void loadReport() {
    activity.record("report requested");
    panel
        .get()
        .host()
        .fetchText(
            "/slow",
            _ -> activity.record("report loaded"),
            failure -> activity.record("report failed " + failure.status()));
  }
}
