package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.core.BrowserEvent;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.EventOptions;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.wasm.BrowserRoot;
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

  private IntoElement view(BrowserRoot root, DocumentContext context) {
    // One render without the once listener re-arms it on the next.
    var bindOnce = !rearming;
    if (rearming) {
      rearming = false;
      root.invalidate();
    }
    var once = Elements.button().attr("type", "button").attr("data-once", "").text("Once");
    if (bindOnce) {
      once.onEvent("click", EventOptions.DEFAULT.withOnce(), _ -> log(root, "once " + ++onceCount));
    }
    return Elements.section()
        .attr("data-events-log", String.join(",", log))
        .children(
            Elements.h2().text("Events"),
            Elements.div()
                .attr("data-phases", "")
                .onEvent(
                    "click",
                    EventOptions.DEFAULT.withCapture(),
                    event -> {
                      log(root, "capture");
                      if (stopInCapture) {
                        event.stopPropagation();
                      }
                    })
                .onEvent("click", _ -> log(root, "bubble"))
                .onEvent("click capture", _ -> log(root, "custom"))
                .child(
                    Elements.button()
                        .attr("type", "button")
                        .text("Phase target")
                        .onEvent(
                            "click", event -> log(root, "target " + event.pointer().button()))),
            Button.of("Stop in capture").onClick(root.update(() -> stopInCapture = !stopInCapture)),
            checkbox("Prevented", EventOptions.DEFAULT, root),
            checkbox("Passive", EventOptions.DEFAULT.withPassive(), root),
            once,
            Button.of("Re-arm once").onClick(root.update(() -> rearming = true)),
            Elements.input("text")
                .ariaLabel("Keys")
                .onEvent(
                    "keydown",
                    event -> {
                      var key = event.keyboard();
                      log(root, "key " + key.key() + " shift=" + key.shiftKey());
                    }),
            Elements.button()
                .attr("type", "button")
                .text("Keyboard data on click")
                .onEvent("click", BrowserEvent::keyboard),
            Elements.button()
                .attr("type", "button")
                .text("Open events dialog")
                .onEvent(
                    "click",
                    _ -> {
                      log(root, "activation " + userActivation());
                      root.host().showModal(dialog);
                    }),
            Elements.dialog()
                .id(dialog)
                .ariaLabel("Events dialog")
                .child(
                    Elements.button().attr("type", "button").boolAttr("autofocus").text("Dismiss")),
            Button.of("Clear events").onClick(root.update(log::clear)),
            Elements.p().text(String.join(",", log)));
  }

  private IntoElement checkbox(String label, EventOptions options, BrowserRoot root) {
    return Elements.label()
        .children(
            Elements.input("checkbox")
                .ariaLabel(label)
                .onEvent(
                    "click",
                    options,
                    event -> {
                      event.preventDefault();
                      log(
                          root,
                          label.toLowerCase(Locale.ROOT)
                              + " prevented="
                              + event.defaultPrevented());
                    }),
            Elements.span().text(label));
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
