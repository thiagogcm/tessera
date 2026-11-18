package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.wasm.BrowserEvent;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.ControlState;
import dev.nthings.tessera.wasm.Live;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import java.util.List;
import java.util.function.Consumer;

/// Live form bindings with explicit Java state and browser-managed initial values.
public final class ControlledFormDemo {
  private String text = "Initial text";
  private String notes = "Initial notes";
  private boolean checked = true;
  private String choice = "b";
  private List<String> choices = List.of("a", "c");
  private boolean controlled = true;
  private boolean accept = true;
  private boolean reversed;
  private boolean visible = true;
  private boolean feedback;
  private boolean alternateDefaults;
  private boolean handoff;
  private int initialFormKey;
  private String requested = "none";
  private int revision;
  private int renders;
  private String last = "none";
  private int events;
  private ControlState captured = new ControlState("", false, List.of(), false);

  private ControlledFormDemo() {}

  public static BrowserRoot mount(String host) {
    return BrowserRoot.mount(host, new ControlledFormDemo()::view);
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    return LiveElement.of(
            Elements.section()
                .attr("data-form-revision", Integer.toString(revision))
                .attr("data-form-turn", Integer.toString(++renders))
                .child(Elements.h2().text("Controlled form")))
        .children(
            Ui.button("Reset form", root.update(this::reset)),
            Ui.button("Refresh form", root.update(() -> revision++)),
            Ui.button("Toggle edit acceptance", root.update(() -> accept = !accept)),
            Ui.button("Toggle controlled form", root.update(() -> controlled = !controlled)),
            Ui.button("Reverse options", root.update(() -> reversed = !reversed)),
            Ui.button("Remove form controls", root.update(() -> visible = false)),
            Ui.button("Toggle field feedback", root.update(() -> feedback = !feedback)),
            Ui.button(
                "Change initial defaults",
                root.update(() -> alternateDefaults = !alternateDefaults)),
            Ui.button("Replace initial form", root.update(() -> initialFormKey++)),
            Ui.button("Hand off controls", root.update(() -> handoff = true)),
            Ui.button("Unmount form", root::close))
        .child(
            Elements.p()
                .attr("data-form-event", "")
                .text(
                    last
                        + ":"
                        + captured.value()
                        + "; checked="
                        + captured.checked()
                        + "; selected="
                        + String.join(",", captured.selectedValues())
                        + "; composing="
                        + captured.composing()
                        + "; events="
                        + events))
        .child(visible ? fields(root) : Live.fragment())
        .child(
            LiveElement.of(
                    Elements.div()
                        .attr(
                            "data-defaults-version",
                            Integer.toString(initialFormKey)
                                + ":"
                                + alternateDefaults
                                + ":"
                                + reversed))
                .child(Elements.p().attr("data-requested-default-change", "").text(requested))
                .child(initialForm(root).key(Integer.toString(initialFormKey))));
  }

  private LiveContent fields(BrowserRoot root) {
    var input = Forms.text("controlled-text").initialValue("Attribute text");
    var text =
        LiveElement.of(input)
            .on("input", record("text", root, state -> this.text = state.value()))
            .on("compositionstart", record("start", root, _ -> {}))
            .on("compositionend", record("end", root, _ -> {}));
    var textarea =
        Elements.textarea().attr("name", "controlled-notes").initialValue("Default notes");
    var notes =
        LiveElement.of(textarea)
            .on("input", record("notes", root, state -> this.notes = state.value()));
    var checkbox = Elements.input("checkbox").attr("name", "controlled-check").initialChecked(true);
    var check =
        LiveElement.of(checkbox)
            .on("change", record("check", root, state -> checked = state.checked()));
    var select = options(false, reversed).initialValue("a");
    var choice =
        LiveElement.of(select)
            .on("change", record("choice", root, state -> this.choice = state.value()));
    var multiple = options(true, reversed).initialSelectedValues(List.of("b"));
    var choices =
        LiveElement.of(multiple)
            .on("change", record("choices", root, state -> this.choices = state.selectedValues()));
    // Without control, the browser keeps each control's current state.
    if (controlled) {
      text.bindValue(this.text);
      notes.bindValue(this.notes);
      check.bindChecked(checked);
      choice.bindValue(this.choice);
      choices.bindSelectedValues(this.choices);
    }
    return LiveElement.of(Elements.form().attr("data-controlled-fields", ""))
        .child(Elements.button().attr("type", "reset").text("Reset native controlled form"))
        .children(
            field("Controlled text", input, text),
            field("Controlled notes", textarea, notes),
            field("Controlled check", checkbox, check),
            field("Controlled choice", select, choice),
            field("Controlled choices", multiple, choices),
            LiveContent.of(browserText()),
            ownershipProbe());
  }

  /// A later sibling's default can change a group's native state before Java applies bindings.
  private LiveContent ownershipProbe() {
    var legacyText =
        LiveElement.of(
            Forms.text("legacy-text")
                .ariaLabel("Legacy bound text")
                .attr("value", handoff ? "Legacy changed" : "Legacy start"));
    var legacyNotes = Forms.textarea("legacy-notes", 2).ariaLabel("Legacy notes");
    if (handoff) {
      legacyNotes.initialValue("New notes default");
    } else {
      legacyNotes.text("Original notes");
    }
    var legacyCheck =
        LiveElement.of(
            Elements.input("checkbox")
                .ariaLabel("Legacy bound check")
                .onlyIf(!handoff, element -> element.boolAttr("checked")));
    var first =
        LiveElement.of(
            Elements.input("radio")
                .attr("name", "bound-radio")
                .ariaLabel("First bound radio")
                .initialChecked(false));
    if (!handoff) {
      legacyText.bindValue("Legacy start");
      legacyCheck.bindChecked(true);
      first.bindChecked(true);
    }
    var second =
        LiveElement.of(
                Elements.input("radio")
                    .attr("name", "bound-radio")
                    .ariaLabel("Second bound radio")
                    .initialChecked(true))
            .bindChecked(false);
    var leading =
        LiveElement.of(
                Elements.input("radio")
                    .attr("name", "bound-radio")
                    .ariaLabel("New bound radio")
                    .initialChecked(true))
            .bindChecked(false);
    return LiveElement.of(Elements.fieldset())
        .children(
            legacyText,
            LiveContent.of(legacyNotes),
            legacyCheck,
            handoff ? leading.key("new") : Live.fragment(),
            first.key("first"),
            second.key("second"));
  }

  /// The same portable form is mounted live and compiled on the showcase's static test route.
  public static TagElement nativeDefaults() {
    var fields = initialControls(false, false);
    return nativeForm()
        .children(
            fields.text(),
            fields.notes(),
            fields.check(),
            fields.firstRadio(),
            fields.secondRadio(),
            fields.choice(),
            fields.choices());
  }

  private LiveContent initialForm(BrowserRoot root) {
    var controls = initialControls(alternateDefaults, reversed);
    return LiveElement.of(nativeForm())
        .children(
            LiveElement.of(controls.text())
                .onValueChange(value -> recordRequest(root, "value:" + value)),
            LiveContent.of(controls.notes()),
            LiveElement.of(controls.check())
                .onCheckedChange(value -> recordRequest(root, "checked:" + value)),
            LiveContent.of(controls.firstRadio()),
            LiveContent.of(controls.secondRadio()),
            LiveContent.of(controls.choice()),
            LiveElement.of(controls.choices())
                .onSelectedValuesChange(
                    values -> recordRequest(root, "selected:" + String.join(",", values))));
  }

  private void recordRequest(BrowserRoot root, String change) {
    requested = change;
    root.invalidate();
  }

  private static TagElement nativeForm() {
    return Elements.form()
        .attr("data-initial-form", "")
        .child(
            Elements.input("hidden")
                .attr("name", "serialized")
                .initialValue("a\r\nb\rc" + (char) 0 + "d"))
        .child(Elements.button().attr("type", "reset").text("Reset browser defaults"));
  }

  private record InitialFields(
      TagElement text,
      TagElement notes,
      TagElement check,
      TagElement firstRadio,
      TagElement secondRadio,
      TagElement choice,
      TagElement choices) {}

  private static InitialFields initialControls(boolean alternate, boolean reverse) {
    return new InitialFields(
        Forms.text("initial-text")
            .ariaLabel("Default text")
            .initialValue(alternate ? "Replacement" : "Portable <value>"),
        Forms.textarea("initial-notes", 2)
            .ariaLabel("Default notes")
            .initialValue(alternate ? "Replacement notes" : "\r\nPortable & notes"),
        Elements.input("checkbox").ariaLabel("Default check").initialChecked(!alternate),
        Elements.input("radio")
            .attr("name", "default-radio")
            .ariaLabel("Default radio one")
            .initialChecked(!alternate),
        Elements.input("radio")
            .attr("name", "default-radio")
            .ariaLabel("Default radio two")
            .initialChecked(alternate),
        options(false, reverse).ariaLabel("Default choice").initialValue(alternate ? "c" : "b"),
        options(true, reverse)
            .ariaLabel("Default choices")
            .initialSelectedValues(alternate ? List.of("b") : List.of("a", "c")));
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

  /// An uncontrolled field whose hint and error come and go around its retained control.
  private Field browserText() {
    var field =
        Field.of("Browser text", Forms.text("browser-text").initialValue("Browser default"));
    return feedback
        ? field.hint("Shown before the control").error("Shown after the control")
        : field;
  }

  private static TagElement options(boolean multiple, boolean reversed) {
    var select =
        Elements.select()
            .attr("name", multiple ? "choices" : "choice")
            .onlyIf(multiple, element -> element.boolAttr("multiple"));
    for (var value : reversed ? List.of("c", "b", "a") : List.of("a", "b", "c")) {
      select.child(Elements.option().attr("value", value).text(value).key(value));
    }
    return select;
  }

  /// Records every event and applies `apply` only while edits are accepted.
  private Consumer<BrowserEvent> record(
      String name, BrowserRoot root, Consumer<ControlState> apply) {
    return event ->
        root.update(
                () -> {
                  captured = event.control();
                  last = name;
                  events++;
                  if (accept) {
                    apply.accept(captured);
                  }
                })
            .run();
  }

  private void reset() {
    text = "";
    notes = "";
    checked = false;
    choice = "c";
    choices = List.of();
    revision++;
  }
}
