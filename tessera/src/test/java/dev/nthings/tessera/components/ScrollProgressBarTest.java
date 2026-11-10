package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.TimelineAxis;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ScrollProgressBar")
class ScrollProgressBarTest {

  private static RenderedPage page;

  @BeforeAll
  static void renderBar() {
    page = renderMain(ScrollProgressBar.of());
  }

  @Test
  @DisplayName("declares the animation shorthand before the scroll timeline")
  void animationShorthandPrecedesTimeline() {
    var interactive = page.ruleOf(page.element("main > div"), "i-");
    var media = interactive.indexOf("@media (prefers-reduced-motion: no-preference)");
    var animation = interactive.indexOf("animation:tessera-scroll-progress auto linear");
    var timeline = interactive.indexOf("animation-timeline:scroll(root block)");

    assertThat(media).isNotNegative();
    assertThat(animation).isGreaterThan(media).isLessThan(timeline);
    assertThat(page.css())
        .contains("@keyframes tessera-scroll-progress")
        .contains("transform:scaleX(0)")
        .contains("transform:scaleX(1)");
  }

  @Test
  @DisplayName("is hidden from assistive technology and fills the top edge")
  void hiddenFromAssistiveTechnology() {
    var bar = page.element("main > div");

    assertThat(bar).hasAttribute("aria-hidden", "true");
    assertThat(page.ruleOf(bar, "u-"))
        .contains("position:fixed")
        .contains("inset-block-start:0px")
        .contains("inset-inline-start:0px")
        .contains("inline-size:100%")
        .contains("block-size:0.25rem")
        .contains("z-index:50")
        .contains("background:var(--accent-primary)");
  }

  @Test
  @DisplayName("honors a custom color, thickness and axis")
  void honorsCustomAppearance() {
    var custom =
        renderMain(
            ScrollProgressBar.of()
                .color(Tokens.STATUS_DANGER)
                .thickness(CssLength.px(3))
                .axis(TimelineAxis.INLINE));
    var bar = custom.element("main > div");

    assertThat(custom.ruleOf(bar, "u-"))
        .contains("block-size:3px")
        .contains("background:var(--status-danger)");
    assertThat(custom.ruleOf(bar, "i-")).contains("animation-timeline:scroll(root inline)");
  }
}
