package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.Live;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import java.util.ArrayList;

/// A session panel that signs itself out from an effect start, an effect cleanup or an after-render
/// task, and whose effects can fail to start or to clean up. The shared [ActivityLog] shows what
/// started and what was cleaned up.
final class ClosureDemo {
  private final ActivityLog activity;
  private boolean guard = true;
  private boolean closeOnStart;
  private boolean failStart;
  private boolean failingCleanups;

  ClosureDemo(ActivityLog activity) {
    this.activity = activity;
  }

  BrowserRoot mount(String host) {
    var mounted = BrowserRoot.mount(host, this::view);
    mounted.lifetime().onDispose(() -> activity.record("session closed"));
    return mounted;
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    var children = new ArrayList<LiveContent>();
    children.add(LiveContent.of(Elements.h2().text("Session")));
    // Its cleanup signs out, so a render that removes it closes the root while committing.
    if (guard) {
      children.add(
          Live.effect(
              "guard",
              lifetime ->
                  lifetime.onDispose(() -> {
                    activity.record("guard cleaned up");
                    root.close();
                  })));
    }
    if (closeOnStart) {
      children.add(
          Live.effect(
              "closing",
              lifetime -> {
                activity.record("closing effect started");
                lifetime.onDispose(() -> activity.record("closing effect cleaned up"));
                root.close();
              }));
      children.add(Live.effect("later", _ -> activity.record("later effect started")));
    }
    if (failStart) {
      children.add(
          Live.effect(
              "failing",
              lifetime -> {
                lifetime.onDispose(() -> activity.record("failing effect cleaned up"));
                throw new IllegalStateException("Simulated effect start failure");
              }));
    }
    if (failingCleanups) {
      children.add(
          Live.effect(
              "victim",
              lifetime ->
                  lifetime.onDispose(() -> {
                    activity.record("victim cleaned up");
                    throw new IllegalStateException("Simulated cleanup failure");
                  })));
      children.add(
          Live.effect(
              "survivor",
              lifetime -> lifetime.onDispose(() -> activity.record("survivor cleaned up"))));
    }
    children.add(Ui.button("Close from cleanup", root.update(() -> guard = false)));
    children.add(Ui.button("Close from effect start", root.update(() -> closeOnStart = true)));
    children.add(
        Ui.button(
            "Close after render",
            () -> {
              root.host()
                  .afterRender(() -> {
                    activity.record("first after-render task ran");
                    root.close();
                  });
              root.host().afterRender(() -> activity.record("second after-render task ran"));
            }));
    children.add(Ui.button("Fail effect start", root.update(() -> failStart = true)));
    children.add(Ui.button("Add failing cleanups", root.update(() -> failingCleanups = true)));
    children.add(Ui.button("Remove failing cleanups", root.update(() -> failingCleanups = false)));
    children.add(Ui.button("Sign out", root::close));
    return LiveElement.of(Elements.section().ariaLabel("Session")).children(children);
  }
}
