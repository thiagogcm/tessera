package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.code;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DescriptionList")
class DescriptionListTest {

  @Test
  @DisplayName("renders each term and description as a dt/dd pair in order")
  void rendersTermDescriptionPairs() {
    var page =
        renderMain(
            DescriptionList.of()
                .term("Runtime", "Latest Chromium + Firefox")
                .term("Language", "Java 25"));

    assertThat(page.elements("main > dl > dt"))
        .extracting(Element::text)
        .containsExactly("Runtime", "Language");
    assertThat(page.elements("main > dl > dd"))
        .extracting(Element::text)
        .containsExactly("Latest Chromium + Firefox", "Java 25");
  }

  @Test
  @DisplayName("keeps markup in a description")
  void keepsMarkupInDescription() {
    var page =
        renderMain(
            DescriptionList.of()
                .term("Compile", p().text("Run ").child(code().text("just verify"))));

    assertThat(page.element("main > dl > dd p > code")).hasText("just verify");
  }

  @Test
  @DisplayName("lays terms and descriptions out on one two-column grid")
  void laysOutOnOneGrid() {
    var page = renderMain(DescriptionList.of().term("Runtime", "Latest Chromium + Firefox"));

    assertThat(page.ruleOf(page.element("dl"), "u-"))
        .contains("display:grid")
        .contains("grid-template-columns:minmax(8rem, max-content) 1fr")
        .contains("gap:var(--space-3)");
  }
}
