package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("layout components")
class LayoutComponentsTest {

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  @DisplayName("a grid rejects a non-positive column count")
  void gridRejectsNonPositiveColumnCount(int columns) {
    assertThatIllegalArgumentException().isThrownBy(() -> Grid.columns(columns));
  }

  @Test
  @DisplayName("a sticky header places its content in a header landmark")
  void stickyHeaderIsAHeader() {
    var page = renderMain(StickyHeader.of(a("/").text("Tessera")));

    assertThat(page.element("header a")).hasAttribute("href", "/").hasText("Tessera");
  }

  @Test
  @DisplayName("the scroll progress bar is hidden from assistive technology")
  void scrollProgressBarIsHidden() {
    assertThat(renderMain(ScrollProgressBar.of()).element("main > *"))
        .hasAttribute("aria-hidden", "true");
  }

  @Test
  @DisplayName("the scroll progress bar declares its animation before the scroll timeline")
  void animationShorthandPrecedesTimeline() {
    var page = renderMain(ScrollProgressBar.of());

    var interactive = page.ruleOf(page.element("main > *"), "i-");
    var media = interactive.indexOf("@media (prefers-reduced-motion: no-preference)");
    var animation = interactive.indexOf("animation:tessera-scroll-progress ");
    var timeline = interactive.indexOf("animation-timeline:scroll(");
    assertThat(media).isNotNegative();
    assertThat(animation).isGreaterThan(media).isLessThan(timeline);
    assertThat(page.css()).contains("@keyframes tessera-scroll-progress");
  }
}
