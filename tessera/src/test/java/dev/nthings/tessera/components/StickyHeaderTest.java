package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StickyHeader")
class StickyHeaderTest {

  @Test
  @DisplayName("wraps content in a sticky header surface")
  void wrapsContentInStickySurface() {
    var page =
        renderMain(
            StickyHeader.of(div().attr("data-brand", "tessera").text("Tessera")).id("site-header"));

    var wrapper = page.element("main > div");
    assertThat(wrapper).hasAttribute("data-scope-id", "site-header");
    assertThat(page.element("main > div > header.sticky-surface > div[data-brand=tessera]"))
        .hasText("Tessera");
  }

  @Test
  @DisplayName("pins the header and blurs its surface once stuck")
  void pinsAndBlursWhenStuck() {
    var page = renderMain(StickyHeader.of(div().text("Tessera")).id("site-header"));
    var scope = page.scopeBody(page.element("header.sticky-surface"));

    assertThat(scope)
        .contains("position:sticky")
        .contains("inset-block-start:0px")
        .contains("z-index:30")
        .contains("container-name:tessera-sticky-header")
        .contains("container-type:scroll-state")
        .contains("@container tessera-sticky-header scroll-state(stuck: top)")
        .contains("backdrop-filter:blur(12px)")
        .contains("background:color-mix(in oklch, var(--surface-card) 88%, transparent)")
        .contains("box-shadow:0 6px 20px -4px oklch(0% 0 0 / 0.14)");
  }

  @Test
  @DisplayName("numbers unnamed headers on the page")
  void numbersUnnamedHeaders() {
    var page = renderMain(StickyHeader.of(div().text("One")), StickyHeader.of(div().text("Two")));

    assertThat(page.elements("main > div"))
        .extracting(header -> header.attr("data-scope-id"))
        .containsExactly("sticky-header-1", "sticky-header-2");
  }
}
