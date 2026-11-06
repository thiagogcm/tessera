package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Forms")
class FormsTest {

  @Nested
  @DisplayName("switch")
  class Switches {

    @Test
    @DisplayName("is a role=switch checkbox reflecting its checked state")
    void reflectsCheckedState() {
      var page = renderMain(Forms.switchInput("enable_notifications", true));

      assertThat(page.element("input[role=switch][name=enable_notifications]"))
          .hasAttribute("checked");
    }

    @Test
    @DisplayName("a switch field is unchecked when asked to be, and labelled for its switch")
    void switchFieldIsLabelledForItsSwitch() {
      var page = renderMain(Forms.switchField("Receive updates", "updates", false));

      var rowSwitch = page.element("input[role=switch][name=updates]");
      assertThat(rowSwitch).doesNotHaveAttribute("checked");
      assertThat(page.element("label")).hasAttribute("for", rowSwitch.id());
    }
  }

  @Nested
  @DisplayName("customizable select")
  class CustomizableSelect {

    private static RenderedPage page;

    @BeforeAll
    static void renderSelect() {
      page =
          renderMain(
              Forms.selectControl("theme_mode")
                  .option("light", "Light Mode")
                  .option("dark", "Dark Mode", true)
                  .option("system", "System Default"));
    }

    @Test
    @DisplayName("adds a selectedcontent trigger as the first child of the <select>")
    void addsSelectedContentTrigger() {
      // Jsoup's legacy parser hoists <button> out of <select>, so match the emitted markup
      assertThat(page.html())
          .containsPattern(
              "<select[^>]*name=\"theme_mode\"[^>]*><button[^>]*class=\"select-trigger\"[^>]*>"
                  + "<selectedcontent></selectedcontent></button>");
    }

    @Test
    @DisplayName("keeps native options and marks only the initial one selected")
    void marksOnlyInitialOptionSelected() {
      assertThat(page.elements("select[name=theme_mode] option[selected]"))
          .extracting(o -> o.attr("value"))
          .containsExactly("dark");
    }

    @Test
    @DisplayName("opts into the base-select appearance")
    void optsIntoBaseSelectAppearance() {
      assertThat(page.css()).contains("appearance:base-select");
    }
  }

  @Nested
  @DisplayName("field")
  class Fields {

    private static RenderedPage page;

    @BeforeAll
    static void renderField() {
      page =
          renderMain(
              Forms.field("Username", Forms.text("username"))
                  .hint("3-20 characters.")
                  .error("Username is taken.")
                  .anchorTooltip("Alphanumeric only"));
    }

    @Test
    @DisplayName("labels its control")
    void labelsItsControl() {
      assertThat(page.element("label"))
          .hasAttribute("for", page.element("input[name=username]").id());
    }

    @Test
    @DisplayName("describes its control by the hint")
    void describesControlByHint() {
      assertThat(page.element("input[name=username]"))
          .hasAttribute("aria-describedby", page.element(".field-hint").id());
    }

    @Test
    @DisplayName("places the error directly after the control, as its :user-invalid styling needs")
    void placesErrorDirectlyAfterControl() {
      var input = page.element("input[name=username]");
      var error = input.nextElementSibling();

      assertThat(error).hasClasses("field-error");
      assertThat(input).hasAttribute("aria-errormessage", error.id());
    }

    @Test
    @DisplayName("anchor-positions its tooltip against the control's anchor name")
    void anchorPositionsTooltipAgainstControl() {
      var input = page.element("input[name=username]");
      var tooltip = page.element(".field-anchor-tooltip[role=tooltip]");
      var anchor = "--anchor-" + input.id();

      assertThat(page.ruleOf(input, "u-")).contains("anchor-name:" + anchor);
      assertThat(page.ruleOf(tooltip, "u-")).contains("position-anchor:" + anchor);
    }
  }

  @Test
  @DisplayName("same-named fields get distinct compiler-assigned scopes")
  void sameNamedFieldsDoNotCollide() {
    var page =
        renderMain(
            Forms.field("Email", Forms.email("email")),
            Forms.field("Newsletter email", Forms.email("email")));

    var labels = page.elements("label.field-label");
    assertThat(labels).hasSize(2);
    assertThat(labels)
        .extracting(label -> label.attr("for"))
        .doesNotHaveDuplicates()
        .allSatisfy(page::elementById);
  }

  @Test
  @DisplayName("a textarea grows with its content via field-sizing")
  void textareaGrowsWithContent() {
    var page = renderMain(Forms.textareaControl("m", 3));

    assertThat(page.css()).contains("field-sizing:content").contains("min-block-size:calc(3lh");
  }
}
