package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Accordion")
class AccordionTest {

  @Test
  @DisplayName("an exclusive accordion shares one name and opens a single item")
  void exclusiveAccordionOpensSingleItem() {
    var page =
        renderMain(
            Accordion.of()
                .id("faq")
                .exclusive()
                .item("Question 1", "Answer 1", true)
                .item("Question 2", "Answer 2", false));

    assertThat(page.elements("details[name=faq]")).hasSize(2);
    assertThat(page.elements("details[name=faq][open]")).hasSize(1);
  }

  @Test
  @DisplayName("exclusive accordions without ids get distinct groups on one page")
  void exclusiveAccordionsWithoutIdsGetDistinctGroups() {
    var page =
        renderMain(
            Accordion.of().exclusive().item("A", "a", false),
            Accordion.of().exclusive().item("B", "b", false),
            Accordion.of().item("C", "c", false));

    assertThat(page.elements("details"))
        .extracting(d -> d.attr("name"))
        .containsExactly("accordion-1", "accordion-2", "");
  }
}
