package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.BrowserEvent;
import dev.nthings.tessera.core.ControlState;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.wasm.BrowserRoot;
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

  private IntoElement view(BrowserRoot root, DocumentContext context) {
    return Elements.section()
        .attr("data-form-revision", Integer.toString(revision))
        .attr("data-form-turn", Integer.toString(++renders))
        .children(
            Elements.h2().text("Controlled form"),
            Button.of("Reset form").onClick(root.update(this::reset)),
            Button.of("Refresh form").onClick(root.update(() -> revision++)),
            Button.of("Toggle edit acceptance").onClick(root.update(() -> accept = !accept)),
            Button.of("Toggle controlled form")
                .onClick(root.update(() -> controlled = !controlled)),
            Button.of("Reverse options").onClick(root.update(() -> reversed = !reversed)),
            Button.of("Remove form controls").onClick(root.update(() -> visible = false)),
            Button.of("Toggle field feedback").onClick(root.update(() -> feedback = !feedback)),
            Button.of("Unmount form").onClick(root::close),
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
                        + events),
            visible ? fields(root) : Elements.fragment());
  }

  private IntoElement fields(BrowserRoot root) {
    var input =
        Forms.text("controlled-text")
            .attr("value", "Attribute text")
            .value(text)
            .onEvent("input", record("text", root, state -> text = state.value()))
            .onEvent("compositionstart", record("start", root, _ -> {}))
            .onEvent("compositionend", record("end", root, _ -> {}));
    var textarea =
        Elements.textarea()
            .attr("name", "controlled-notes")
            .text("Default notes")
            .value(notes)
            .onEvent("input", record("notes", root, state -> notes = state.value()));
    var checkbox =
        Elements.input("checkbox")
            .attr("name", "controlled-check")
            .boolAttr("checked")
            .checked(checked)
            .onEvent("change", record("check", root, state -> checked = state.checked()));
    var select =
        options(false)
            .value(choice)
            .onEvent("change", record("choice", root, state -> choice = state.value()));
    var multiple =
        options(true)
            .selectedValues(choices)
            .onEvent("change", record("choices", root, state -> choices = state.selectedValues()));
    if (!controlled) {
      for (var control : List.of(input, textarea, checkbox, select, multiple)) {
        control.uncontrolled();
      }
    }
    return Elements.fragment()
        .children(
            Field.of("Controlled text", input),
            Field.of("Controlled notes", textarea),
            Field.of("Controlled check", checkbox),
            Field.of("Controlled choice", select).id("controlled-choice"),
            Field.of("Controlled choices", multiple).id("controlled-choices"),
            browserText());
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
