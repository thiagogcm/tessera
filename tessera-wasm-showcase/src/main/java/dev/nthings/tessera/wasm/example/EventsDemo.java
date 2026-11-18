package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.EventOptions;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import java.util.ArrayList;
import java.util.List;

/// Native event handling: a capture-phase guard on a toolbar, synchronous default prevention,
/// keyboard data and a one-shot listener, each reported in a visible event log.
final class EventsDemo {
  private final List<String> log = new ArrayList<>();
  private boolean readOnly;
  private boolean rearming;
  private boolean claimed;
  private int rewards;

  static BrowserRoot mount(String host) {
    return BrowserRoot.mount(host, new EventsDemo()::view);
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    // One render without the one-shot listener arms it again on the next.
    var armed = !rearming;
    if (rearming) {
      rearming = false;
      root.invalidate();
    }
    // While it is being offered again the button has no listener; once claimed, the spent
    // listener stays bound until the reward is offered again.
    var reward =
        LiveElement.of(
            Elements.button()
                .onlyIf(!armed, button -> button.boolAttr("disabled"))
                .text(!armed ? "Preparing reward" : claimed ? "Reward claimed" : "Claim reward"));
    if (armed) {
      reward.on(
          "click",
          EventOptions.DEFAULT.withOnce(),
          _ ->
              root.update(
                      () -> {
                        claimed = true;
                        log.add("reward " + ++rewards);
                      })
                  .run());
    }
    var entries = Elements.ol().ariaLabel("Event log");
    log.forEach(entry -> entries.child(Elements.li().text(entry)));
    return LiveElement.of(
            Elements.section().ariaLabel("Events").child(Elements.h2().text("Events")))
        .child(
            LiveElement.of(Elements.div().ariaLabel("Toolbar"))
                .on(
                    "click",
                    EventOptions.DEFAULT.withCapture(),
                    event -> {
                      if (readOnly) {
                        event.stopPropagation();
                        log(root, "blocked");
                      }
                    })
                .child(Ui.button("Delete", () -> log(root, "deleted"))))
        .children(
            LiveElement.of(Elements.label())
                .child(
                    LiveElement.of(Elements.input("checkbox"))
                        .on("change", _ -> root.update(() -> readOnly = !readOnly).run()))
                .text("Read-only"),
            LiveElement.of(Elements.label())
                .child(
                    LiveElement.of(Elements.input("checkbox"))
                        .on(
                            "click",
                            event -> {
                              event.preventDefault();
                              log(root, "terms must be read first");
                            }))
                .text("Accept terms"),
            LiveElement.of(Elements.input("text").ariaLabel("Shortcut"))
                .on(
                    "keydown",
                    event -> {
                      var key = event.keyboard();
                      log(root, (key.shiftKey() ? "Shift+" : "") + key.key());
                    }),
            reward,
            Ui.button(
                "Offer another reward",
                root.update(() -> {
                  rearming = true;
                  claimed = false;
                })))
        .child(entries);
  }

  private void log(BrowserRoot root, String entry) {
    root.update(() -> log.add(entry)).run();
  }
}
