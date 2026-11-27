package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.IntoElement;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Accordion")
class AccordionTest {

  @Test
  @DisplayName("an exclusive accordion groups all its items and opens only the open item")
  void exclusiveOpensSingleItem() {
    var page =
        renderMain(
            Accordion.of()
                .scopeId("faq")
                .exclusive()
                .openItem("Question 1", "Answer 1")
                .item("Question 2", "Answer 2"));

    var items = page.elements("details");
    assertThat(items)
        .extracting(item -> item.attr("name"))
        .doesNotContain("")
        .containsOnly(items.getFirst().attr("name"));
    assertThat(items).extracting(item -> item.hasAttr("open")).containsExactly(true, false);
  }

  @Test
  @DisplayName("a non-exclusive accordion lets items open independently")
  void plainAccordionHasNoGroup() {
    var page = renderMain(Accordion.of().item("A", "a").item("B", "b"));

    assertThat(page.elements("details")).hasSize(2).noneMatch(item -> item.hasAttr("name"));
  }

  static Stream<Arguments> separateAccordions() {
    var shared = Accordion.of().scopeId("faq").exclusive().item("Refunds?", "Within 30 days.");
    return Stream.of(
        argumentSet(
            "two unnamed accordions",
            Accordion.of().exclusive().item("A", "a"),
            Accordion.of().exclusive().item("B", "b")),
        argumentSet(
            "one id in two cards",
            Card.of("Pricing")
                .scopeId("pricing")
                .body(Accordion.of().scopeId("faq").exclusive().item("A", "a")),
            Card.of("Support")
                .scopeId("support")
                .body(Accordion.of().scopeId("faq").exclusive().item("B", "b"))),
        argumentSet(
            "one instance placed in two cards",
            Card.of("Pricing").scopeId("pricing").body(shared),
            Card.of("Support").scopeId("support").body(shared)));
  }

  @ParameterizedTest
  @MethodSource("separateAccordions")
  @DisplayName("separate exclusive accordions never share a page-wide group")
  void separateAccordionsUseDistinctGroups(IntoElement first, IntoElement second) {
    var page = renderMain(first, second);

    assertThat(page.elements("details"))
        .extracting(item -> item.attr("name"))
        .hasSize(2)
        .doesNotContain("")
        .doesNotHaveDuplicates();
  }
}
