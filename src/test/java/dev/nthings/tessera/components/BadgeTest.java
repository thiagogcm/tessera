package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Badge")
class BadgeTest {

  private static Element render(Badge badge) {
    return renderMain(badge).element(".badge");
  }

  @Test
  @DisplayName("is a neutral, tinted, medium span by default")
  void rendersNeutralTintedSpanByDefault() {
    var badge = render(Badge.of("Status"));

    assertThat(badge)
        .hasTagName("span")
        .hasText("Status")
        .hasClasses("badge--neutral", "badge--tinted", "badge--md")
        .doesNotHaveClasses("is-pill");
    assertThat(badge.select(".badge-dot")).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "NEUTRAL, badge--neutral",
    "ACCENT, badge--accent",
    "SUCCESS, badge--success",
    "WARNING, badge--warning",
    "DANGER, badge--danger",
    "INFO, badge--info"
  })
  @DisplayName("tone selects its modifier class")
  void toneSelectsModifierClass(Badge.Tone tone, String modifier) {
    assertThat(render(Badge.of("Tone").tone(tone))).hasClasses(modifier);
  }

  @ParameterizedTest
  @CsvSource({"TINTED, badge--tinted", "OUTLINE, badge--outline", "SOLID, badge--solid"})
  @DisplayName("variant selects its modifier class")
  void variantSelectsModifierClass(Badge.Variant variant, String modifier) {
    assertThat(render(Badge.of("Variant").variant(variant))).hasClasses(modifier);
  }

  @ParameterizedTest
  @CsvSource({"SM, badge--sm", "MD, badge--md", "LG, badge--lg"})
  @DisplayName("size selects its modifier class")
  void sizeSelectsModifierClass(Badge.Size size, String modifier) {
    assertThat(render(Badge.of("Size").size(size))).hasClasses(modifier);
  }

  @Test
  @DisplayName("fluent shortcuts combine tone, variant, size and pill styling")
  void fluentShortcutsCombineModifiers() {
    var badge = render(Badge.of("Critical").danger().solid().lg().pill());

    assertThat(badge).hasClasses("badge--danger", "badge--solid", "badge--lg", "is-pill");
  }

  @Test
  @DisplayName("a dot adds a decorative indicator beside the label")
  void dotAddsDecorativeIndicator() {
    var badge = render(Badge.dot("Online", Badge.Tone.SUCCESS));

    assertThat(badge).hasClasses("badge--success");
    assertThat(badge.selectFirst(".badge-dot")).hasAttribute("aria-hidden", "true");
    assertThat(badge.selectFirst(".badge-label")).hasText("Online");
  }

  @Test
  @DisplayName("an href turns the badge into an anchor")
  void hrefRendersAnchor() {
    var link = renderMain(Badge.accent("Docs").href("/docs")).element("a.badge");

    assertThat(link).hasAttribute("href", "/docs").hasClasses("badge--accent").hasText("Docs");
  }
}
