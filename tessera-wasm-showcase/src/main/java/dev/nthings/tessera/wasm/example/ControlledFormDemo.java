package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.wasm.BrowserEvent;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.ControlState;
import dev.nthings.tessera.wasm.Live;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import dev.nthings.tessera.wasm.LiveField;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/// A profile form whose controls are bound to Java state and previewed as they change. Locking the
/// form rejects edits without a render, as an application that only re-renders on change does. Text
/// waits for IME composition to end before it is accepted. The form autosaves every minute, which
/// re-renders it while the user keeps editing.
final class ControlledFormDemo {
  private String text = "Initial text";
  private String notes = "Initial notes";
  private boolean checked = true;
  private String choice = "b";
  private List<String> choices = List.of("a", "c");
  private boolean controlled = true;
  private boolean locked;
  private int autosaves;
  private boolean usernameInvalid;

  static BrowserRoot mount(String host) {
    return BrowserRoot.mount(host, new ControlledFormDemo()::view);
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    return LiveElement.of(
            Elements.section()
                .ariaLabel("Profile")
                .children(
                    Elements.h2().text("Profile"),
                    Elements.p()
                        .role(Role.STATUS)
                        .ariaLabel("Preview")
                        .text(
                            "Text: "
                                + text
                                + "; notes: "
                                + notes
                                + "; checked: "
                                + checked
                                + "; choice: "
                                + choice
                                + "; choices: "
                                + String.join(",", choices)),
                    Elements.p().text("Autosaved " + autosaves + " times")))
        .children(
            Live.effect(
                "autosave",
                lifetime ->
                    root.host()
                        .within(lifetime)
                        .interval(Duration.ofMinutes(1), () -> autosaves++)),
            toggle("Lock form", locked, root, value -> locked = value),
            toggle(
                "Let the browser own the fields", !controlled, root, value -> controlled = !value),
            Ui.button("Clear form", root.update(this::clear)),
            fields(root));
  }

  private LiveContent fields(BrowserRoot root) {
    var input = Forms.text("controlled-text").initialValue("Default text");
    var text = LiveElement.of(input).on("input", edit(root, state -> this.text = state.value()));
    var textarea =
        Elements.textarea().attr("name", "controlled-notes").initialValue("Default notes");
    var notes =
        LiveElement.of(textarea).on("input", edit(root, state -> this.notes = state.value()));
    var checkbox = Elements.input("checkbox").attr("name", "controlled-check").initialChecked(true);
    var check =
        LiveElement.of(checkbox).on("change", edit(root, state -> checked = state.checked()));
    var select = options("choice", false).initialValue("a");
    var choice =
        LiveElement.of(select).on("change", edit(root, state -> this.choice = state.value()));
    var multiple = options("choices", true).initialSelectedValues(List.of("b"));
    var choices =
        LiveElement.of(multiple)
            .on("change", edit(root, state -> this.choices = state.selectedValues()));
    if (controlled) {
      text.bindValue(this.text);
      notes.bindValue(this.notes);
      check.bindChecked(checked);
      choice.bindValue(this.choice);
      choices.bindSelectedValues(this.choices);
    }
    var username =
        LiveField.of(
                Field.of("Username", Forms.text("username")).hint("Shown on your public profile"),
                control ->
                    control.on(
                        "input",
                        event -> {
                          var invalid = event.control().value().length() < 3;
                          if (invalid != usernameInvalid) {
                            root.update(() -> usernameInvalid = invalid).run();
                          }
                        }))
            .validation(usernameInvalid, "Use at least 3 characters");
    return LiveElement.of(Elements.form().ariaLabel("Profile form"))
        .children(
            field("Controlled text", input, text),
            field("Controlled notes", textarea, notes),
            field("Controlled check", checkbox, check),
            field("Controlled choice", select, choice),
            field("Controlled choices", multiple, choices),
            username,
            LiveContent.of(Elements.button().attr("type", "reset").text("Reset to defaults")));
  }

  /// Applies an accepted edit and renders it. A locked form, or text still being composed, is
  /// ignored without a render.
  private Consumer<BrowserEvent> edit(BrowserRoot root, Consumer<ControlState> apply) {
    return event -> {
      var state = event.control();
      if (!locked && !state.composing()) {
        apply.accept(state);
        root.invalidate();
      }
    };
  }

  private static LiveContent toggle(
      String label, boolean value, BrowserRoot root, Consumer<Boolean> change) {
    return LiveElement.of(Elements.label())
        .child(
            LiveElement.of(Elements.input("checkbox"))
                .bindChecked(value)
                .onCheckedChange(checked -> root.update(() -> change.accept(checked)).run()))
        .text(label);
  }

  /// A labelled field around a live `control` wrapping `host`.
  private static LiveContent field(String label, TagElement host, LiveElement control) {
    return LiveElement.of(
            Elements.div()
                .flex()
                .flexCol()
                .gap(Tokens.SPACE_1)
                .child(Elements.label().labelFor(host.ref()).text(label)))
        .child(control);
  }

  private static TagElement options(String name, boolean multiple) {
    var select =
        Elements.select()
            .attr("name", name)
            .onlyIf(multiple, element -> element.boolAttr("multiple"));
    for (var value : List.of("a", "b", "c")) {
      select.child(Elements.option().attr("value", value).text(value).key(value));
    }
    return select;
  }

  private void clear() {
    text = "";
    notes = "";
    checked = false;
    choice = "c";
    choices = List.of();
  }
}
