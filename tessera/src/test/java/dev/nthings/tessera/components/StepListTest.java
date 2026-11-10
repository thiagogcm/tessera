package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StepList")
class StepListTest {

  @Test
  @DisplayName("renders numbered steps in order")
  void rendersNumberedSteps() {
    var page =
        renderMain(
            StepList.of("Run just verify", "Run just site", "Upload target/site to any static host")
                .id("publish-steps"));

    assertThat(page.element("main > ol")).hasAttribute("data-scope-id", "publish-steps");
    assertThat(page.elements("main > ol > li"))
        .extracting(Element::text)
        .containsExactly(
            "Run just verify", "Run just site", "Upload target/site to any static host");
  }

  @Test
  @DisplayName("styles steps with a counter and a progress-tinted badge")
  void stylesStepsWithCounterAndTintedBadge() {
    var page = renderMain(StepList.of("One", "Two").id("steps"));
    var scope = page.scopeBody(page.element("ol"));

    assertThat(scope)
        .contains("counter-reset:step")
        .contains("counter-increment:step")
        .contains("--step-index:sibling-index()")
        .contains("--step-total:sibling-count()")
        .contains("content:counter(step)")
        .contains(
            "background:color-mix(in oklab, var(--accent-primary)"
                + " calc(20% + ((60% * var(--step-index)) / var(--step-total))),"
                + " var(--surface-subtle))");
  }

  @Test
  @DisplayName("numbers unnamed step lists on the page")
  void numbersUnnamedStepLists() {
    var page = renderMain(StepList.of("One"), StepList.of("Two"));

    assertThat(page.elements("main > ol"))
        .extracting(list -> list.attr("data-scope-id"))
        .containsExactly("step-list-1", "step-list-2");
  }

  @Test
  @DisplayName("keeps markup in steps")
  void keepsMarkupSteps() {
    var page =
        renderMain(
            StepList.of(div().text("Write components"), a("/docs").text("Compile the site")));

    assertThat(page.element("main > ol > li:first-child > div")).hasText("Write components");
    assertThat(page.element("main > ol > li:last-child > a"))
        .hasAttribute("href", "/docs")
        .hasText("Compile the site");
  }
}
