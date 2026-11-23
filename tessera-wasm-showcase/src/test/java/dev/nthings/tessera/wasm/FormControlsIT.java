package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;

/// Form controls bound to Java state or owned by the browser, on the forms page.
@DisplayName("Form controls")
class FormControlsIT {

  @BothEngines
  @DisplayName("edits reach Java and an application reset clears only the bound controls")
  void editsRoundTripThroughJava(Session session) {
    session.open("/forms/");
    var preview = session.role(AriaRole.STATUS, "Preview");
    var text = session.field("Controlled text");
    var notes = session.field("Controlled notes");
    var check = session.field("Controlled check");
    var choice = session.field("Controlled choice");
    var choices = session.field("Controlled choices");
    var username = session.field("Username");
    assertThat(text).hasValue("Initial text");
    assertThat(notes).hasValue("Initial notes");
    assertThat(check).isChecked();
    assertThat(choice).hasValue("b");
    assertThat(choices).hasValues(new String[] {"a", "c"});

    username.fill("ada");
    text.fill("Java text");
    notes.fill("Java notes");
    check.uncheck();
    choice.selectOption("a");
    choices.selectOption(new String[] {"b", "c"});
    assertThat(preview)
        .hasText("Text: Java text; notes: Java notes; checked: false; choice: a; choices: b,c");

    session.button("Clear form").click();
    assertThat(text).hasValue("");
    assertThat(notes).hasValue("");
    assertThat(check).not().isChecked();
    assertThat(choice).hasValue("c");
    assertThat(choices).hasValues(new String[] {});
    assertThat(username).hasValue("ada");
  }

  @BothEngines
  @DisplayName("an unrelated render keeps focus and the selection, and typing continues from it")
  void unrelatedRendersKeepTheCaret(Session session) {
    var page = session.page();
    page.clock().install();
    session.open("/forms/");
    var autosaves = 0;
    for (var label : List.of("Controlled text", "Controlled notes")) {
      var control = session.field(label);
      control.fill("abcdef");
      Session.select(control, 2, 4, "backward");
      page.clock().runFor(60_000);
      assertThat(session.text("Autosaved " + ++autosaves + " times")).isVisible();
      assertThat(control).isFocused();
      Assertions.assertThat(Session.selection(control)).isEqualTo(List.of(2, 4, "backward"));
      page.keyboard().type("Z");
      assertThat(control).hasValue("abZef");
      Assertions.assertThat(Session.caret(control)).isEqualTo(List.of(3, 3));
    }
    assertThat(session.role(AriaRole.STATUS, "Preview")).containsText("Text: abZef; notes: abZef");
  }

  @BothEngines
  @DisplayName("edits the application rejects without rendering are reverted")
  void rejectedEditsRevert(Session session) {
    session.open("/forms/");
    var preview = session.role(AriaRole.STATUS, "Preview");
    var text = session.field("Controlled text");
    var check = session.field("Controlled check");
    var choices = session.field("Controlled choices");
    assertThat(text).hasValue("Initial text");
    var before = preview.textContent();
    session.field("Lock form").check();

    text.click();
    session.page().keyboard().press("End");
    session.page().keyboard().type("!");
    assertThat(text).hasValue("Initial text");
    check.click();
    assertThat(check).isChecked();
    choices.selectOption(new String[] {"b"});
    assertThat(choices).hasValues(new String[] {"a", "c"});
    assertThat(preview).hasText(before);
  }

  @BothEngines
  @DisplayName("browser-owned controls keep the user's state until Java binds them again")
  void browserOwnedControlsKeepTheirState(Session session) {
    session.open("/forms/");
    var text = session.field("Controlled text");
    var notes = session.field("Controlled notes");
    var check = session.field("Controlled check");
    var choice = session.field("Controlled choice");
    var choices = session.field("Controlled choices");
    text.fill("Kept on handoff");
    session.field("Let the browser own the fields").check();
    assertThat(text).hasValue("Kept on handoff");

    session.field("Lock form").check();
    text.fill("Browser owns text");
    notes.fill("Browser owns notes");
    check.uncheck();
    choice.selectOption("a");
    choices.selectOption(new String[] {"b"});
    session.button("Clear form").click();
    assertThat(text).hasValue("Browser owns text");
    assertThat(notes).hasValue("Browser owns notes");
    assertThat(check).not().isChecked();
    assertThat(choice).hasValue("a");
    assertThat(choices).hasValues(new String[] {"b"});

    session.field("Let the browser own the fields").uncheck();
    assertThat(text).hasValue("");
    assertThat(notes).hasValue("");
    assertThat(choice).hasValue("c");
    assertThat(choices).hasValues(new String[] {});
  }

  @BothEngines
  @DisplayName("live validation shows and clears its error around the focused field")
  void validationKeepsTheFieldBeingEdited(Session session) {
    session.open("/forms/");
    var username = session.field("Username");
    username.click();
    session.page().keyboard().type("ad");
    assertThat(username).hasAccessibleErrorMessage("Use at least 3 characters");
    assertThat(username).hasAttribute("aria-invalid", "true");
    assertThat(username).hasAccessibleDescription("Shown on your public profile");
    assertThat(username).isFocused();
    assertThat(username).hasValue("ad");
    Assertions.assertThat(Session.caret(username)).isEqualTo(List.of(2, 2));

    session.page().keyboard().type("a");
    assertThat(username).not().hasAttribute("aria-invalid", "true");
    assertThat(session.text("Use at least 3 characters")).hasCount(0);
    assertThat(username).isFocused();
    assertThat(username).hasValue("ada");
    Assertions.assertThat(Session.caret(username)).isEqualTo(List.of(3, 3));
  }

  @BothEngines
  @DisplayName(
      "portable defaults submit and read like the static form, survive renders and reset natively")
  void portableDefaultsMatchStaticOutput(Session session) {
    session.open("/forms/static/");
    var form = session.role(AriaRole.FORM, "Defaults form");
    var defaults = formData(form);
    var compiledTree = form.ariaSnapshot();
    Assertions.assertThat(defaults)
        .isEqualTo(
            List.of(
                "serialized=a\r\nb\rc d",
                "text=Portable <value>",
                "notes=\nPortable & notes",
                "check=on",
                "radio=one",
                "choice=b",
                "choices=a",
                "choices=c"));

    session.open("/forms/");
    Assertions.assertThat(formData(form)).isEqualTo(defaults);
    Assertions.assertThat(form.ariaSnapshot()).isEqualTo(compiledTree);
    session.field("Default text").fill("Browser edit");
    assertThat(session.text("Last change: text")).isVisible();
    session.field("Default notes").fill("Edited notes");
    session.field("Default check").uncheck();
    assertThat(session.text("Last change: check")).isVisible();
    session.field("Default radio two").check();
    session.field("Default choice").selectOption("a");
    session.field("Default choices").selectOption(new String[] {"b"});
    assertThat(session.text("Last change: choices")).isVisible();
    var edited =
        List.of(
            "serialized=a\r\nb\rc d",
            "text=Browser edit",
            "notes=Edited notes",
            "radio=two",
            "choice=a",
            "choices=b");
    Assertions.assertThat(formData(form)).isEqualTo(edited);

    session.button("Reset browser defaults").click();
    Assertions.assertThat(formData(form)).isEqualTo(defaults);

    session.button("Replace with alternate defaults").click();
    assertThat(session.field("Default text")).hasValue("Replacement");
    Assertions.assertThat(formData(form))
        .isEqualTo(
            List.of(
                "serialized=a\r\nb\rc d",
                "text=Replacement",
                "notes=Replacement notes",
                "radio=two",
                "choice=c",
                "choices=b"));
  }

  @BothEngines
  @DisplayName("options that load later keep the user's choice and select the declared default")
  void lateOptionsKeepChoicesAndApplyDefaults(Session session) {
    var page = session.page();
    page.clock().install();
    session.open("/forms/");
    var pickup = session.field("Pickup");
    pickup.check();
    assertThat(session.text("Delivery: pickup")).isVisible();

    page.clock().runFor(60_000);
    var express = session.field("Express");
    assertThat(express).isVisible();
    assertThat(pickup).isChecked();
    assertThat(express).not().isChecked();
    assertThat(session.text("Delivery: pickup")).isVisible();
    assertThat(session.field("Country")).hasValue("br");
  }

  /// The form's entries as a submission would encode them: a list of `name=value` strings.
  private static Object formData(Locator form) {
    return form.evaluate(
        "form => [...new FormData(form)].map(([name, value]) => name + '=' + value)");
  }
}
