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
        .child(visible ? fields(root) : Live.fragment());
  }

  private LiveContent fields(BrowserRoot root) {
    var input = Forms.text("controlled-text").attr("value", "Attribute text");
    var text =
        LiveElement.of(input)
            .on("input", record("text", root, state -> this.text = state.value()))
            .on("compositionstart", record("start", root, _ -> {}))
            .on("compositionend", record("end", root, _ -> {}));
    var textarea = Elements.textarea().attr("name", "controlled-notes").text("Default notes");
    var notes =
        LiveElement.of(textarea)
            .on("input", record("notes", root, state -> this.notes = state.value()));
    var checkbox = Elements.input("checkbox").attr("name", "controlled-check").boolAttr("checked");
    var check =
        LiveElement.of(checkbox)
            .on("change", record("check", root, state -> checked = state.checked()));
    var select = options(false);
    var choice =
        LiveElement.of(select)
            .on("change", record("choice", root, state -> this.choice = state.value()));
    var multiple = options(true);
    var choices =
        LiveElement.of(multiple)
            .on("change", record("choices", root, state -> this.choices = state.selectedValues()));
    // Without control, the browser keeps each control's current state.
    if (controlled) {
      text.value(this.text);
      notes.value(this.notes);
      check.checked(checked);
      choice.value(this.choice);
      choices.selectedValues(this.choices);
    }
    return Live.fragment(
        field("Controlled text", input, text),
        field("Controlled notes", textarea, notes),
        field("Controlled check", checkbox, check),
        field("Controlled choice", select, choice),
        field("Controlled choices", multiple, choices),
        LiveContent.of(browserText()));
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
        Field.of("Browser text", Forms.text("browser-text").attr("value", "Browser default"));
    return feedback
        ? field.hint("Shown before the control").error("Shown after the control")
        : field;
  }

  private TagElement options(boolean multiple) {
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
