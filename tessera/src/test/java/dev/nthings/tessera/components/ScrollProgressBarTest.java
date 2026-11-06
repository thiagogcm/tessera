package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ScrollProgressBar")
class ScrollProgressBarTest {

  private static RenderedPage page;

  @BeforeAll
  static void renderBar() {
    page = renderMain(new ScrollProgressBar("top-progress"));
  }

  @Test
  @DisplayName("declares the animation shorthand before the scroll timeline that it would reset")
  void declaresAnimationShorthandBeforeTimeline() {
    var animationIndex = page.css().indexOf("animation:tessera-scroll-progress");
    var timelineIndex = page.css().indexOf("animation-timeline:scroll(root block)");

    assertThat(animationIndex).isPositive().isLessThan(timelineIndex);
  }

  @Test
  @DisplayName("is hidden from assistive technology")
  void isHiddenFromAssistiveTechnology() {
    assertThat(page.elementById("top-progress")).hasAttribute("aria-hidden", "true");
  }
}
