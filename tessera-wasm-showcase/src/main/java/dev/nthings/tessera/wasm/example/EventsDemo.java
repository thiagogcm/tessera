package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.wasm.BrowserEvent;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.EventOptions;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.graalvm.webimage.api.JS;

/// Event contract evidence: phases, options, typed data, default prevention and activation.
public final class EventsDemo {
  private final List<String> log = new ArrayList<>();
  private final ElementRef dialog = ElementRef.scoped("dialog");
  private boolean stopInCapture;
  private boolean rearming;
  private int onceCount;

  private EventsDemo() {}

  public static BrowserRoot mount(String host) {
    return BrowserRoot.mount(host, new EventsDemo()::view);
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    // One render without the once listener re-arms it on the next.
    var bindOnce = !rearming;
    if (rearming) {
      rearming = false;
      root.invalidate();
    }
    var once =
        LiveElement.of(Elements.button().attr("type", "button").attr("data-once", "").text("Once"));
    if (bindOnce) {
      once.on("click", EventOptions.DEFAULT.withOnce(), _ -> log(root, "once " + ++onceCount));
    }
    return LiveElement.of(
            Elements.section()
                .attr("data-events-log", String.join(",", log))
                .child(Elements.h2().text("Events")))
        .child(
            LiveElement.of(Elements.div().attr("data-phases", ""))
                .on(
                    "click",
                    EventOptions.DEFAULT.withCapture(),
                    event -> {
                      log(root, "capture");
                      if (stopInCapture) {
                        event.stopPropagation();
                      }
                    })
                .on("click", _ -> log(root, "bubble"))
                .on("click capture", _ -> log(root, "custom"))
                .child(
                    LiveElement.of(Elements.button().attr("type", "button").text("Phase target"))
                        .on("click", event -> log(root, "target " + event.pointer().button()))))
        .children(
            Ui.button("Stop in capture", root.update(() -> stopInCapture = !stopInCapture)),
            checkbox("Prevented", EventOptions.DEFAULT, root),
            checkbox("Passive", EventOptions.DEFAULT.withPassive(), root),
            once,
            Ui.button("Re-arm once", root.update(() -> rearming = true)),
            LiveElement.of(Elements.input("text").ariaLabel("Keys"))
                .on(
                    "keydown",
                    event -> {
                      var key = event.keyboard();
                      log(root, "key " + key.key() + " shift=" + key.shiftKey());
                    }),
            LiveElement.of(Elements.button().attr("type", "button").text("Keyboard data on click"))
                .on("click", BrowserEvent::keyboard),
            LiveElement.of(Elements.button().attr("type", "button").text("Open events dialog"))
                .on(
                    "click",
                    _ -> {
                      log(root, "activation " + userActivation());
                      root.host().showModal(dialog);
                    }))
        .child(
            Elements.dialog()
                .id(dialog)
                .ariaLabel("Events dialog")
                .child(
                    Elements.button().attr("type", "button").boolAttr("autofocus").text("Dismiss")))
        .child(Ui.button("Clear events", root.update(log::clear)))
        .child(Elements.p().text(String.join(",", log)));
  }

  private LiveContent checkbox(String label, EventOptions options, BrowserRoot root) {
    return LiveElement.of(Elements.label())
        .child(
            LiveElement.of(Elements.input("checkbox").ariaLabel(label))
                .on(
                    "click",
                    options,
                    event -> {
                      event.preventDefault();
                      log(
                          root,
                          label.toLowerCase(Locale.ROOT)
                              + " prevented="
                              + event.defaultPrevented());
                    }))
        .child(Elements.span().text(label));
  }

  /// Test probe: `true` while the event's user activation is active, or `unsupported`.
  @JS.Coerce
  @JS(
      "return navigator.userActivation ? String(navigator.userActivation.isActive) : 'unsupported';")
  private static native String userActivation();

  private void log(BrowserRoot root, String entry) {
    root.update(() -> log.add(entry)).run();
  }
}
