package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Modal;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.InputType;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.Events;
import dev.nthings.tessera.wasm.FetchResult;
import dev.nthings.tessera.wasm.FocusReturn;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import java.time.Duration;
import org.jspecify.annotations.Nullable;

/// Browser services through element references: focus return, text selection, dialogs, popovers,
/// media, timers and text requests, each surviving updates and released when the root closes.
final class ServicesDemo {
  private final ElementRef draft = ElementRef.scoped("draft");
  private final ElementRef dialog = ElementRef.scoped("dialog");
  private final ElementRef opener = ElementRef.scoped("opener");
  private final ElementRef menu = ElementRef.scoped("menu");
  private final ElementRef audio = ElementRef.scoped("audio");
  private final ElementRef confirm = ElementRef.scoped("confirm");
  private int renders;
  private int generation;
  private String status = "idle";
  private @Nullable FocusReturn focus;

  private ServicesDemo() {}

  static BrowserRoot mount(String host) {
    return BrowserRoot.mount(host, new ServicesDemo()::view);
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    renders++;
    var host = root.host();
    // Rebuilt on every render, as declarative views do. The first takes an application-owned
    // reference; the second keeps the reference it creates, which the next render replaces.
    var owned =
        Modal.of("Confirm later")
            .scopeId("confirm")
            .bindDialog(confirm)
            .bodyText("Opened by a timer");
    var rebuilt = Modal.of("Stale later").scopeId("stale").bodyText("Never opened");
    return LiveElement.of(
            Elements.section()
                .ariaLabel("Host services")
                .children(
                    Elements.h2().text("Host services"),
                    Elements.p().role(Role.STATUS).text(status),
                    Elements.input(InputType.TEXT)
                        .id(draft)
                        .ariaLabel("Draft")
                        .attr("value", "Hello services")))
        .children(
            Ui.button("Select first word", () -> host.select(draft, 0, 5)),
            Ui.button(
                "Read selection",
                root.update(() -> {
                  var selection = host.selection(draft);
                  status = "selection " + selection.start() + "-" + selection.end();
                })),
            Ui.button("Re-render services", root.update(() -> {})),
            LiveElement.of(
                    Elements.button()
                        .id(opener)
                        .attr("data-generation", Integer.toString(generation))
                        .text("Open services dialog"))
                .on(
                    Events.CLICK,
                    _ -> {
                      focus = host.rememberFocus();
                      host.showModal(dialog);
                    })
                .key("opener-" + generation),
            LiveElement.of(
                    Elements.dialog()
                        .id(dialog)
                        .ariaLabel("Services dialog")
                        .child(Elements.p().text("Renders: " + renders)))
                .children(
                    Ui.button("Re-render inside dialog", root.update(() -> {})),
                    Ui.button(
                        "Close services dialog",
                        () -> {
                          host.close(dialog);
                          restoreFocus();
                        }),
                    Ui.button(
                        "Close and replace opener",
                        () -> {
                          generation++;
                          host.close(dialog);
                          // The new opener exists only after the next commit.
                          host.afterRender(this::restoreFocus);
                        })),
            Ui.button("Toggle menu", () -> host.togglePopover(menu)))
        .child(Elements.div().id(menu).attr("popover", "manual").text("Menu"))
        .child(Elements.audio().id(audio).attr("src", "/tone.wav").boolAttr("loop"))
        .children(
            Ui.button(
                "Play audio",
                () -> {
                  host.muted(audio, true);
                  host.play(
                      audio, () -> status = "playing", reason -> status = "rejected " + reason);
                }),
            Ui.button("Pause audio", root.update(() -> host.pause(audio))),
            Ui.button(
                "Read media",
                root.update(() -> {
                  var media = host.media(audio);
                  status =
                      "media paused=" + media.paused() + " advanced=" + (media.currentTime() > 0);
                })))
        .child(owned)
        .child(rebuilt)
        .children(
            Ui.button(
                "Remind me in 5 seconds",
                root.update(
                    () -> host.timeout(Duration.ofSeconds(5), () -> host.showModal(confirm)))),
            // A reference created by a render that later renders replace: wrong on purpose.
            Ui.button(
                "Remind me through a stale reference",
                root.update(() -> {
                  var stale = rebuilt.dialogRef();
                  host.timeout(Duration.ofSeconds(5), () -> host.showModal(stale));
                })),
            Ui.button(
                "Load greeting",
                () -> host.fetchText("/text", result -> status = formatFetch(result))),
            Ui.button(
                "Load missing page",
                () -> host.fetchText("/missing", result -> status = formatFetch(result))),
            Ui.button("Close services root", root::close));
  }

  private static String formatFetch(FetchResult result) {
    return switch (result) {
      case FetchResult.Ok ok -> "Loaded: " + ok.body();
      case FetchResult.HttpError error -> "Failed with HTTP " + error.status();
      case FetchResult.NetworkError error -> "Failed: " + error.message();
    };
  }

  private void restoreFocus() {
    var saved = focus;
    focus = null;
    if (saved != null) {
      saved.restore(opener);
    }
  }
}
