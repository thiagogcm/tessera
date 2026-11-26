package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.support.AccessibleText;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ColorSchemeToggle")
class ColorSchemeToggleTest {

  private static RenderedPage page;

  @BeforeAll
  static void renderToggles() {
    page = renderMain(ColorSchemeToggle.of(), ColorSchemeToggle.of());
  }

  @Test
  @DisplayName("each toggle is a named button inside the tessera-color-scheme element")
  void rendersNamedButton() {
    var buttons = page.elements("main > tessera-color-scheme > button[data-color-scheme-toggle]");

    assertThat(buttons).hasSize(2);
    assertThat(AccessibleText.name(buttons.getFirst())).isEqualTo("Color scheme: system");
    assertThat(buttons.getFirst().select("svg"))
        .extracting(svg -> svg.attr("aria-hidden"))
        .containsExactly("true", "true", "true");
  }

  @Test
  @DisplayName("the page restores the saved scheme once, in head, before its stylesheet")
  void restoresSavedSchemeInHead() {
    var restore = page.dom().head().select("script:not([type=module])");

    assertThat(restore).hasSize(1);
    assertThat(restore.getFirst().data()).contains("tessera-color-scheme", "colorScheme");
    assertThat(page.html()).containsSubsequence("tessera-color-scheme\")", "<style>");
    assertThat(page.html()).contains("customElements.define(\"tessera-color-scheme\"");
  }

  @Test
  @DisplayName("the stylesheet sets color-scheme from the root's data-color-scheme")
  void stylesheetFollowsChoice() {
    assertThat(page.css())
        .contains(":root[data-color-scheme=light]{color-scheme:light}")
        .contains(":root[data-color-scheme=dark]{color-scheme:dark}");
  }

  @Test
  @DisplayName("a page without a toggle loads neither script")
  void noToggleNoScript() {
    var plain = renderMain(Badge.of("New"));

    assertThat(plain.html()).doesNotContain("tessera-color-scheme");
  }
}
