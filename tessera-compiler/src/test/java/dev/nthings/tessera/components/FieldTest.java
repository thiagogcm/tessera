package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.InputType;
import dev.nthings.tessera.core.IntoElement;
import java.util.List;
import java.util.stream.Stream;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("form fields")
class FieldTest {

  static Stream<Arguments> labelledControls() {
    return Stream.of(
        argumentSet("text input", Field.of("Username", Forms.input("username", InputType.TEXT))),
        argumentSet("textarea", Field.of("Username", Forms.textarea("username", 3))),
        argumentSet("select", Field.of("Username", Select.of("username").option("ada", "Ada"))),
        argumentSet("switch", Forms.switchField("Username", Forms.switchInput("username"))));
  }

  @ParameterizedTest
  @MethodSource("labelledControls")
  @DisplayName("the visible label names its control")
  void labelsItsControl(IntoElement field) {
    var page = renderMain(field);

    assertThat(page.target(page.element("label"), "for")).isSameAs(page.element("[name=username]"));
    assertThat(page.element("label")).hasText("Username");
  }

  @Test
  @DisplayName("the hint describes the control and the error is its error message")
  void hintAndErrorDescribeTheControl() {
    var page =
        renderMain(
            Field.of("Username", Forms.input("username", InputType.TEXT))
                .hint("3-20 characters.")
                .error("Username is taken.")
                .tooltip("Alphanumeric only"));

    var input = page.element("input[name=username]");
    assertThat(page.target(input, "aria-describedby")).hasText("3-20 characters.");
    assertThat(page.target(input, "aria-errormessage").text()).contains("Username is taken.");
  }

  @Test
  @DisplayName("explicit invalidity marks the control and links its message")
  void explicitInvalidity() {
    var page =
        renderMain(
            Field.of("Email", Forms.input("email", InputType.EMAIL))
                .error("Enter an email.")
                .validation(new Field.Validation(true, "Already registered.")));

    var input = page.element("input[name=email]");
    assertThat(input).hasAttribute("aria-invalid", "true");
    assertThat(page.target(input, "aria-errormessage").text())
        .contains("Already registered.")
        .doesNotContain("Enter an email.");
  }

  @Test
  @DisplayName("explicit validity marks the control valid and shows no message")
  void explicitValidity() {
    var page =
        renderMain(
            Field.of("Email", Forms.input("email", InputType.EMAIL))
                .error("Enter an email.")
                .validation(new Field.Validation(false, "Already registered.")));

    var input = page.element("input[name=email]");
    assertThat(input)
        .hasAttribute("aria-invalid", "false")
        .doesNotHaveAttribute("aria-errormessage");
    assertThat(page.element("main").text())
        .doesNotContain("Enter an email.", "Already registered.");
  }

  @ParameterizedTest
  @MethodSource("switches")
  @DisplayName("a switch field starts in its declared state")
  void switchStartsInDeclaredState(IntoElement field, boolean checked) {
    var input = renderMain(field).element("input[name=updates]");

    assertThat(input).hasAttribute("role", "switch");
    assertThat(input.hasAttr("checked")).isEqualTo(checked);
  }

  static Stream<Arguments> switches() {
    return Stream.of(
        argumentSet(
            "off", Forms.switchField("Receive updates", Forms.switchInput("updates")), false),
        argumentSet(
            "on",
            Forms.switchField("Receive updates", Forms.switchInput("updates").initialChecked(true)),
            true));
  }

  @Test
  @DisplayName("same-named fields get distinct ids that each label resolves")
  void sameNamedFieldsGetDistinctIds() {
    var page =
        renderMain(
            Field.of("Email", Forms.input("email", InputType.EMAIL)),
            Field.of("Newsletter email", Forms.input("email", InputType.EMAIL)));

    var inputs = page.elements("input[name=email]");
    assertThat(page.elements("label"))
        .extracting(label -> page.target(label, "for"))
        .containsExactlyElementsOf(inputs);
  }

  @Test
  @DisplayName("one control reused across fields follows each field's hint and error")
  void reusedControlFollowsEachField() {
    var control = Forms.input("name", InputType.TEXT);
    var described = Field.of("Name", control).hint("Full name").error("Name is required");
    var plain = Field.of("Name", control);

    for (var field : List.of(described, plain)) {
      var page = renderMain(field);
      var input = page.element("input[name=name]");
      assertThat(page.target(page.element("label"), "for")).isSameAs(input);
      if (field == described) {
        assertThat(page.target(input, "aria-describedby")).hasText("Full name");
        assertThat(page.target(input, "aria-errormessage").text()).contains("Name is required");
      } else {
        assertThat(input)
            .doesNotHaveAttribute("aria-describedby")
            .doesNotHaveAttribute("aria-errormessage");
        assertThat(page.element("main").text()).isEqualTo("Name");
      }
    }
  }

  static Stream<Arguments> initialSelections() {
    return Stream.of(
        argumentSet(
            "plain options",
            Select.of("size")
                .initialValue("large")
                .option("small", "Small")
                .option("large", "Large")),
        argumentSet(
            "option in a group",
            Select.of("size")
                .initialValue("large")
                .option("small", "Small")
                .optgroup(
                    "Big",
                    List.of(
                        Select.Option.of("medium", "Medium"),
                        Select.Option.of("large", "Large")))));
  }

  @ParameterizedTest
  @MethodSource("initialSelections")
  @DisplayName("a select marks only its initial value selected, on every render")
  void marksOnlyInitialOptionSelected(Select select) {
    var field = Field.of("Size", select).hint("Choose a size");
    for (var render = 0; render < 2; render++) {
      var page = renderMain(field);
      var control = page.element("select");
      assertThat(page.elements("option[selected]"))
          .extracting(Element::val)
          .containsExactly("large");
      assertThat(page.target(page.element("label"), "for")).isSameAs(control);
      assertThat(page.target(control, "aria-describedby")).hasText("Choose a size");
    }
  }
}
