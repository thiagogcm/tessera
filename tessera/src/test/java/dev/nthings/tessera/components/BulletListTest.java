package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BulletList")
class BulletListTest {

  @Test
  @DisplayName("wraps plain text items in list items")
  void wrapsPlainTextItems() {
    var page = renderMain(BulletList.of("Zero JavaScript", "Scoped styles", "Shared stylesheet"));

    assertThat(page.element("main > ul")).hasTagName("ul");
    assertThat(page.elements("main > ul > li"))
        .extracting(Element::text)
        .containsExactly("Zero JavaScript", "Scoped styles", "Shared stylesheet");
    assertThat(page.ruleOf(page.element("ul"), "u-"))
        .contains("display:flex")
        .contains("flex-direction:column")
        .contains("gap:var(--space-2)")
        .contains("padding-inline:var(--space-5)");
  }

  @Test
  @DisplayName("keeps markup in list items")
  void keepsMarkupItems() {
    var page = renderMain(BulletList.of(a("/docs").text("Docs"), div().text("Scoped styles")));

    assertThat(page.element("main > ul > li:first-child > a"))
        .hasAttribute("href", "/docs")
        .hasText("Docs");
    assertThat(page.element("main > ul > li:last-child > div")).hasText("Scoped styles");
  }
}
