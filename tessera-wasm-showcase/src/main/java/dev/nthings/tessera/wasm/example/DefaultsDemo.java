package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.InputType;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import java.util.List;

/// A browser-owned form declared with portable initial values. The static form route compiles the
/// same form, so both submit the same data. Each edit is reported to Java, which re-renders the
/// form around the user's values; replacing the form applies the other set of defaults.
public final class DefaultsDemo {
  private boolean alternate;
  private int generation;
  private String lastChange = "none";

  private DefaultsDemo() {}

  static BrowserRoot mount(String host) {
    return BrowserRoot.mount(host, new DefaultsDemo()::view);
  }

  /// The form as static pages compile it.
  public static TagElement staticForm() {
    var controls = controls(false);
    return form()
        .children(
            controls.text(),
            controls.notes(),
            controls.check(),
            controls.firstRadio(),
            controls.secondRadio(),
            controls.choice(),
            controls.choices());
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    var controls = controls(alternate);
    var live =
        LiveElement.of(form())
            .children(
                LiveElement.of(controls.text()).onValueChange(value -> report(root, "text")),
                LiveElement.of(controls.notes()).onValueChange(value -> report(root, "notes")),
                LiveElement.of(controls.check()).onCheckedChange(value -> report(root, "check")),
                LiveContent.of(controls.firstRadio()),
                LiveContent.of(controls.secondRadio()),
                LiveContent.of(controls.choice()),
                LiveElement.of(controls.choices())
                    .onSelectedValuesChange(values -> report(root, "choices")));
    return LiveElement.of(Elements.section().ariaLabel("Defaults"))
        .child(Elements.p().role(Role.STATUS).text("Last change: " + lastChange))
        .child(
            Ui.button(
                "Replace with alternate defaults",
                root.update(() -> {
                  alternate = !alternate;
                  generation++;
                })))
        .child(live.key("form-" + generation));
  }

  private void report(BrowserRoot root, String change) {
    root.update(() -> lastChange = change).run();
  }

  private static TagElement form() {
    return Elements.form()
        .ariaLabel("Defaults form")
        .child(
            Elements.input(InputType.HIDDEN)
                .attr("name", "serialized")
                .initialValue("a\r\nb\rc" + (char) 0 + "d"))
        .child(Elements.button().attr("type", "reset").text("Reset browser defaults"));
  }

  private record Controls(
      TagElement text,
      TagElement notes,
      TagElement check,
      TagElement firstRadio,
      TagElement secondRadio,
      TagElement choice,
      TagElement choices) {}

  private static Controls controls(boolean alternate) {
    return new Controls(
        Elements.input(InputType.TEXT)
            .attr("name", "text")
            .ariaLabel("Default text")
            .initialValue(alternate ? "Replacement" : "Portable <value>"),
        Elements.textarea()
            .attr("name", "notes")
            .ariaLabel("Default notes")
            .initialValue(alternate ? "Replacement notes" : "\r\nPortable & notes"),
        Elements.input(InputType.CHECKBOX)
            .attr("name", "check")
            .ariaLabel("Default check")
            .initialChecked(!alternate),
        Elements.input(InputType.RADIO)
            .attr("name", "radio")
            .attr("value", "one")
            .ariaLabel("Default radio one")
            .initialChecked(!alternate),
        Elements.input(InputType.RADIO)
            .attr("name", "radio")
            .attr("value", "two")
            .ariaLabel("Default radio two")
            .initialChecked(alternate),
        options("choice", false).ariaLabel("Default choice").initialValue(alternate ? "c" : "b"),
        options("choices", true)
            .ariaLabel("Default choices")
            .initialSelectedValues(alternate ? List.of("b") : List.of("a", "c")));
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
}
