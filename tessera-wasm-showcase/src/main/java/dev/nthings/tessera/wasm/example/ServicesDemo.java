package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.components.Modal;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.FocusReturn;
import java.time.Duration;
import org.graalvm.webimage.api.JS;
import org.jspecify.annotations.Nullable;

/// Host-service evidence: focus restoration, selection, dialog, popover and media state across
/// updates and disposal, using only element references.
public final class ServicesDemo {
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

  public static BrowserRoot mount(String host) {
    return BrowserRoot.mount(host, new ServicesDemo()::view);
  }

  private IntoElement view(BrowserRoot root, DocumentContext context) {
    renders++;
    var host = root.host();
    // Rebuilt on every render, as declarative views do. The first takes an application-owned
    // reference; the second keeps the reference it creates, which the next render replaces.
    var owned =
        Modal.of("Confirm later").id("confirm").dialogRef(confirm).bodyText("Opened by a timer");
    var rebuilt = Modal.of("Stale later").id("stale").bodyText("Never opened");
    return Elements.section()
        .attr("data-services-status", status)
        .attr("data-services-renders", Integer.toString(renders))
        .children(
            Elements.h2().text("Host services"),
            Elements.input("text").id(draft).ariaLabel("Draft").attr("value", "Hello services"),
            Button.of("Select first word").onClick(() -> host.select(draft, 0, 5)),
            Button.of("Read selection")
                .onClick(
                    root.update(() -> {
                      var selection = host.selection(draft);
                      status = "selection " + selection.start() + "-" + selection.end();
                    })),
            Button.of("Re-render services").onClick(root.update(() -> {})),
            Elements.button()
                .id(opener)
                .attr("type", "button")
                .attr("data-generation", Integer.toString(generation))
                .text("Open services dialog")
                .onEvent(
                    "click",
                    _ -> {
                      var saved = host.rememberFocus();
                      focus = saved;
                      exposeRestoration(() -> saved.restore(opener));
                      host.showModal(dialog);
                    })
                .key("opener-" + generation),
            Elements.dialog()
                .id(dialog)
                .ariaLabel("Services dialog")
                .children(
                    Elements.p().attr("data-dialog-renders", "").text("Renders: " + renders),
                    Button.of("Re-render inside dialog").onClick(root.update(() -> {})),
                    Button.of("Close services dialog")
                        .onClick(() -> {
                          host.close(dialog);
                          restoreFocus();
                        }),
                    Button.of("Close and replace opener")
                        .onClick(() -> {
                          generation++;
                          host.close(dialog);
                          // The new opener exists only after the next commit.
                          host.afterRender(this::restoreFocus);
                        })),
            Button.of("Toggle menu").onClick(() -> host.togglePopover(menu)),
            Elements.div().id(menu).attr("popover", "auto").text("Menu"),
            Elements.audio().id(audio).attr("src", "/tone.wav").boolAttr("loop"),
            Button.of("Play audio")
                .onClick(() -> {
                  host.muted(audio, true);
                  host.play(
                      audio, () -> status = "playing", reason -> status = "rejected " + reason);
                }),
            Button.of("Pause audio").onClick(root.update(() -> host.pause(audio))),
            Button.of("Read media")
                .onClick(
                    root.update(() -> {
                      var media = host.media(audio);
                      status =
                          "media paused="
                              + media.paused()
                              + " advanced="
                              + (media.currentTime() > 0);
                    })),
            owned,
            rebuilt,
            Button.of("Open overlay later")
                .onClick(
                    root.update(
                        () -> host.timeout(Duration.ofMillis(100), () -> host.showModal(confirm)))),
            Button.of("Open stale overlay later")
                .onClick(
                    root.update(() -> {
                      var stale = rebuilt.dialogRef();
                      host.timeout(Duration.ofMillis(100), () -> host.showModal(stale));
                    })),
            Button.of("Focus unbound reference")
                .onClick(() -> host.focus(ElementRef.scoped("missing"))),
            Button.of("Close services root").onClick(root::close),
            Elements.p().text(status));
  }

  /// Browser acceptance probe for a retained focus token after consumption or root disposal.
  @JS.Coerce
  @JS(
      args = {"restore"},
      value = "globalThis.restoreSavedFocus = () => restore();")
  private static native void exposeRestoration(Runnable restore);

  private void restoreFocus() {
    var saved = focus;
    focus = null;
    if (saved != null) {
      saved.restore(opener);
    }
  }
}
