package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.support.RenderedPage;
import java.util.List;
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
    @DisplayName("reflects its checked state on a role=switch checkbox")
    void reflectsCheckedState() {
      var page =
          renderMain(
              Forms.switchInput("enable_notifications")
                  .boolAttr("checked")
                  .ariaLabel("Notifications"));

      assertThat(page.element("input[role=switch][name=enable_notifications]"))
          .hasAttribute("checked");
    }

    @Test
    @DisplayName("a switch field starts unchecked and its label targets the switch")
    void labelsSwitchField() {
      var page = renderMain(Forms.switchField("Receive updates", "updates"));

      var rowSwitch = page.element("input[role=switch][name=updates]");
      assertThat(rowSwitch).doesNotHaveAttribute("checked");
      assertThat(page.element("label")).hasAttribute("for", rowSwitch.id());
    }

    @Test
    @DisplayName("a checked switch field starts on")
    void checkedFieldStartsOn() {
      var page = renderMain(Forms.checkedSwitchField("Receive updates", "updates"));

      assertThat(page.element("input[role=switch][name=updates]")).hasAttribute("checked");
    }
  }

  @Nested
  @DisplayName("field across renders")
  class RepeatedFields {

    @Test
    @DisplayName("repeated renders follow the current hint and error without mutating the control")
    void repeatedRendersDoNotMutate() {
      var control = Forms.text("name");
      var described =
          Field.of("Name", control)
              .id("name")
              .tooltip("Enter your name")
              .hint("Full name")
              .error("Name is required");
      var plain = Field.of("Name", control).id("name");

      for (var turn = 0; turn < 4; turn++) {
        var enabled = turn % 2 == 0;
        var page = renderMain(enabled ? described : plain);

        var input = page.element("input[name=name]");
        assertThat(page.element("label")).hasAttribute("for", input.id());
        assertThat(page.elements(".field-hint")).hasSize(enabled ? 1 : 0);
        assertThat(page.elements(".field-error")).hasSize(enabled ? 1 : 0);
        if (enabled) {
          assertThat(page.elementById(input.attr("aria-describedby")).text())
              .isEqualTo("Full name");
          assertThat(page.elementById(input.attr("aria-errormessage")).text())
              .contains("Name is required");
        } else {
          assertThat(input).doesNotHaveAttribute("aria-describedby");
          assertThat(input).doesNotHaveAttribute("aria-errormessage");
        }
      }
      assertThat(control.idReferences()).isEmpty();
      assertThat(control.isAnchor()).isFalse();
    }

    @Test
    @DisplayName("a select field keeps its options and hint link across renders")
    void selectFieldKeepsOptions() {
      var select = Select.of("size").option("small", "Small").selectedOption("large", "Large");
      var field = Field.of("Size", select).hint("Choose a size");

      for (var turn = 0; turn < 2; turn++) {
        var page = renderMain(field);
        var element = page.element("select");
        assertThat(page.elements("option")).hasSize(2);
        assertThat(page.element("label")).hasAttribute("for", element.id());
        assertThat(page.elementById(element.attr("aria-describedby")).text())
            .isEqualTo("Choose a size");
      }
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
              Select.of("theme_mode")
                  .option("light", "Light Mode")
                  .selectedOption("dark", "Dark Mode")
                  .option("system", "System Default"));
    }

    @Test
    @DisplayName("emits a select-trigger button with selectedcontent")
    void addsSelectedContentTrigger() {
      // Jsoup's parser hoists <button> out of <select>; inspect the emitted markup.
      assertThat(page.html())
          .contains("class=\"select-trigger\"")
          .contains("Select theme_mode")
          .contains("<selectedcontent>");
    }

    @Test
    @DisplayName("marks only the initial option selected")
    void marksOnlyInitialOptionSelected() {
      assertThat(page.elements("select[name=theme_mode] option[selected]"))
          .extracting(o -> o.attr("value"))
          .containsExactly("dark");
    }

    @Test
    @DisplayName("marks the selected option inside an optgroup")
    void marksSelectedOptgroupOption() {
      var grouped =
          renderMain(
              Select.of("region")
                  .optgroup(
                      "Europe",
                      List.of(
                          Select.Option.of("fra", "Frankfurt"),
                          Select.Option.selected("ams", "Amsterdam"))));

      assertThat(grouped.elements("optgroup[label=Europe] option[selected]"))
          .extracting(o -> o.attr("value"))
          .containsExactly("ams");
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
              Field.of("Username", Forms.text("username"))
                  .hint("3-20 characters.")
                  .error("Username is taken.")
                  .tooltip("Alphanumeric only"));
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
    @DisplayName("places the error directly after the control")
    void placesErrorAfterControl() {
      var input = page.element("input[name=username]");
      var error = input.nextElementSibling();

      assertThat(error).hasClasses("field-error");
      assertThat(input).hasAttribute("aria-errormessage", error.id());
    }

    @Test
    @DisplayName("anchors the tooltip to the control's anchor name")
    void anchorsTooltipToControl() {
      var input = page.element("input[name=username]");
      var tooltip = page.element(".field-anchor-tooltip[role=tooltip]");
      var anchor = "--anchor-" + input.id();

      assertThat(page.ruleOf(input, "u-")).contains("anchor-name:" + anchor);
      assertThat(page.ruleOf(tooltip, "u-")).contains("position-anchor:" + anchor);
    }
  }

  @Test
  @DisplayName("same-named fields get distinct ids that resolve to elements")
  void sameNamedFieldsGetDistinctIds() {
    var page =
        renderMain(
            Field.of("Email", Forms.email("email")),
            Field.of("Newsletter email", Forms.email("email")));

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
    var page = renderMain(Forms.textarea("m", 3));

    assertThat(page.css()).contains("field-sizing:content").contains("min-block-size:calc(3lh");
  }
}
