package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NumberedList")
class NumberedListTest {

  @Test
  @DisplayName("renders an ordered list with tabular numbers")
  void rendersOrderedListWithTabularNumbers() {
    var page = renderMain(NumberedList.of("Describe", "Compile", "Deploy"));

    assertThat(page.element("main > ol")).hasTagName("ol");
    assertThat(page.elements("main > ol > li"))
        .extracting(Element::text)
        .containsExactly("Describe", "Compile", "Deploy");
    assertThat(page.ruleOf(page.element("ol"), "u-")).contains("font-variant-numeric:tabular-nums");
  }

  @Test
  @DisplayName("keeps markup in list items")
  void keepsMarkupItems() {
    var page = renderMain(NumberedList.of(a("/docs/compile").text("Compile the site")));

    assertThat(page.element("main > ol > li > a"))
        .hasAttribute("href", "/docs/compile")
        .hasText("Compile the site");
  }
}
