package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Accordion")
class AccordionTest {

  @Test
  @DisplayName("an exclusive accordion shares one name and opens a single item")
  void exclusiveOpensSingleItem() {
    var page =
        renderMain(
            Accordion.of()
                .id("faq")
                .exclusive()
                .openItem("Question 1", "Answer 1")
                .item("Question 2", "Answer 2"));

    var items = page.elements("details[name=faq]");
    assertThat(items).hasSize(2);
    assertThat(items.get(0)).hasAttribute("open");
    assertThat(items.get(1)).doesNotHaveAttribute("open");
  }

  @Test
  @DisplayName("unnamed exclusive accordions get distinct group names")
  void unnamedAccordionsGetDistinctGroups() {
    var page =
        renderMain(
            Accordion.of().exclusive().item("A", "a"),
            Accordion.of().exclusive().item("B", "b"),
            Accordion.of().item("C", "c"));

    assertThat(page.elements("details"))
        .extracting(d -> d.attr("name"))
        .containsExactly("accordion-1", "accordion-2", "");
  }
}
