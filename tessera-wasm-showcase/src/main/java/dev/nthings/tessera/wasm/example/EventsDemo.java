package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.InputType;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.EventOption;
import dev.nthings.tessera.wasm.Events;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import java.util.ArrayList;
import java.util.List;

/// Native event handling: a capture-phase guard on a toolbar, synchronous default prevention,
/// keyboard data that skips input method composition and a one-shot listener, each reported in a
/// visible event log.
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
          Events.CLICK,
          _ ->
              root.update(
                      () -> {
                        claimed = true;
                        log.add("reward " + ++rewards);
                      })
                  .run(),
          EventOption.ONCE);
    }
    var entries = Elements.ol().ariaLabel("Event log");
    log.forEach(entry -> entries.child(Elements.li(entry)));
    return LiveElement.of(Elements.section(Elements.h2("Events")).ariaLabel("Events"))
        .child(
            LiveElement.of(Elements.div().ariaLabel("Toolbar"))
                .on(
                    Events.CLICK,
                    event -> {
                      if (readOnly) {
                        event.stopPropagation();
                        log(root, "blocked");
                      }
                    },
                    EventOption.CAPTURE)
                .child(Ui.button("Delete", () -> log(root, "deleted"))))
        .children(
            LiveElement.of(Elements.label())
                .child(
                    LiveElement.of(Elements.input(InputType.CHECKBOX))
                        .on(Events.CHANGE, _ -> root.update(() -> readOnly = !readOnly).run()))
                .text("Read-only"),
            LiveElement.of(Elements.label())
                .child(
                    LiveElement.of(Elements.input(InputType.CHECKBOX))
                        .on(
                            Events.CLICK,
                            event -> {
                              event.preventDefault();
                              log(root, "terms must be read first");
                            }))
                .text("Accept terms"),
            // A key that confirms input method text is not a shortcut.
            LiveElement.of(Elements.input(InputType.TEXT).ariaLabel("Shortcut"))
                .on(
                    Events.KEYDOWN,
                    event -> {
                      if (!event.composing()) {
                        log(root, (event.shiftKey() ? "Shift+" : "") + event.key());
                      }
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
