package dev.nthings.tessera.wasm.example;

import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.components.Drawer;
import dev.nthings.tessera.components.Modal;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.InputType;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.Events;
import dev.nthings.tessera.wasm.KeyboardEvent;
import dev.nthings.tessera.wasm.Live;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveDialog;
import dev.nthings.tessera.wasm.LiveDialog.Mode;
import dev.nthings.tessera.wasm.LiveElement;
import java.time.Duration;
import org.jspecify.annotations.Nullable;

/// Dialogs whose open state is bound to the application or owned by the browser: an entry editor
/// that refuses to close with unsaved changes, a session warning a timer opens, a save confirmation
/// whose opener is disabled while saving, a filter drawer the application shows through the host,
/// and dialogs authored open or closed.
///
/// Editor shortcuts work inside the open editor: Ctrl+S saves, Ctrl+Delete removes the entry (and
/// with it the editor's invoker) and Ctrl+D discards the editor.
final class DialogDemo {
  private final ElementRef filtersRef = ElementRef.scoped("filters");
  private final ElementRef entries = ElementRef.scoped("entries");
  private boolean editing;
  private boolean unsaved;
  private boolean showEntry = true;
  private boolean showEditor = true;
  private int dismissals;
  private boolean warning;
  private boolean confirming;
  private boolean saving;
  private int minutes;

  static BrowserRoot mount(String host) {
    return BrowserRoot.mount(host, new DialogDemo()::view);
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    var editor =
        LiveDialog.of(Modal.of("Edit entry").body(p().text("Editing the entry")))
            .bindOpen(editing)
            .onOpenRequest(() -> editing = true)
            .onDismiss(() -> {
              dismissals++;
              if (!unsaved) {
                editing = false;
              }
            })
            .focusFallback(entries);
    var sessionWarning =
        LiveDialog.of(Modal.of("Session expiring").body(p().text("Your session ends soon.")))
            .bindOpen(warning)
            .onDismiss(() -> warning = false)
            .focusFallback(entries);
    // Save stays disabled while it is confirmed and while it saves. Closing the confirmation
    // confirms it.
    var confirmation =
        LiveDialog.of(
                Modal.of("Save changes?")
                    .closeLabel("Confirm")
                    .body(p().text("Saving replaces the published entry.")))
            .bindOpen(confirming)
            .onDismiss(() -> {
              confirming = false;
              saving = true;
            })
            .focusFallback(entries);
    var filters =
        LiveDialog.of(Drawer.of("Filters").dialogRef(filtersRef).body(p().text("Filter options")))
            .mode(Mode.NON_MODAL);
    var tour = LiveDialog.of(Modal.of("Welcome tour").opened().body(p().text("Take the tour")));
    var help = LiveDialog.of(Drawer.of("Help").body(p().text("Help topics")));

    var page =
        LiveElement.of(Elements.section().ariaLabel("Dialogs"))
            .on(Events.KEYDOWN, event -> shortcut(root, event))
            // The session warning opens after an hour; the minute clock updates meanwhile.
            .child(
                Live.effect(
                    "session",
                    lifetime -> {
                      var host = root.host().within(lifetime);
                      host.timeout(Duration.ofHours(1), () -> warning = true);
                      host.interval(Duration.ofMinutes(1), () -> minutes++);
                    }))
            .child(Elements.h2().id(entries).attr("tabindex", "-1").text("Entries"))
            .child(Elements.p().role(Role.STATUS).text("Dismiss attempts: " + dismissals))
            .child(Elements.p().text("Minutes on page: " + minutes))
            .child(
                LiveElement.of(Elements.label())
                    .child(
                        LiveElement.of(Elements.input(InputType.CHECKBOX))
                            .bindChecked(unsaved)
                            .onCheckedChange(value -> root.update(() -> unsaved = value).run()))
                    .text("Unsaved changes"));
    if (showEntry && showEditor) {
      // Keyed, so removing it never hands its node, and the focus it would get back, to a sibling.
      page.child(editor.triggerButton("Edit entry").key("edit-entry"));
    }
    if (showEditor) {
      page.child(editor.key("editor"));
    }
    return page.child(sessionWarning)
        .child(
            LiveElement.of(
                    Elements.button().onlyIf(confirming || saving, el -> el.boolAttr("disabled")))
                .text(saving ? "Saving" : "Save")
                .onClick(root.update(() -> confirming = true)))
        .child(confirmation)
        .child(Ui.button("Show filters", () -> root.host().show(filtersRef)))
        .child(filters)
        .child(tour)
        .child(help.triggerButton("Open help"))
        .child(help);
  }

  private void shortcut(BrowserRoot root, KeyboardEvent event) {
    if (!event.ctrlKey()) {
      return;
    }
    @Nullable Runnable change =
        switch (event.key()) {
          case "s" -> () -> unsaved = false;
          case "Delete" -> () -> showEntry = false;
          case "d" -> () -> showEditor = false;
          default -> null;
        };
    if (change != null) {
      event.preventDefault();
      root.update(change).run();
    }
  }
}
