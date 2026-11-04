package dev.nthings.tessera.css;

import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.section;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.support.CssBlocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ScrollReveal")
class ScrollRevealTest {

  private static final String MOTION_OK = "@media (prefers-reduced-motion: no-preference){";

  @Test
  @DisplayName("scrollReveal drives the animation from a view timeline, gated by reduced motion")
  void scrollRevealUsesViewTimelineBehindReducedMotionQuery() {
    var page =
        renderMain(
            section().scrollReveal(ScrollReveal.FADE_UP).child(p().text("Revealed on scroll")));

    var rule = page.ruleOf(page.element("section"), "i-");

    assertThat(CssBlocks.block(rule, MOTION_OK)).contains("animation-timeline:view(block)");
    assertThat(page.css()).contains("@keyframes tessera-fade-up{");
  }

  @Test
  @DisplayName("revealOnce triggers a one-shot animation, gated by reduced motion")
  void revealOnceTriggersOneShotAnimationBehindReducedMotionQuery() {
    var page = renderMain(p().revealOnce(ScrollReveal.FADE_UP).text("hello"));

    var rule = page.ruleOf(page.element("main p"), "i-");

    assertThat(CssBlocks.block(rule, MOTION_OK))
        .contains("timeline-trigger:")
        .contains("animation-trigger:")
        .contains("play-once");
    assertThat(page.css()).contains("@keyframes tessera-fade-up");
  }
}
