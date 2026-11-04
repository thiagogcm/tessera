package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.css.CssDeclaration;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.TimelineAxis;
import dev.nthings.tessera.css.Tokens;

/// Scroll-driven progress indicator bound to `animation-timeline: scroll(root <axis>)` and gated
/// behind `@media (prefers-reduced-motion: no-preference)`.
///
/// Emits `aria-hidden="true"` so assistive technologies skip the decorative indicator.
public record ScrollProgressBar(
    String id, DesignToken barColor, CssLength thickness, TimelineAxis axis) implements RenderOnce {

  private static final String PROGRESS_KEYFRAME =
      "@keyframes tessera-scroll-progress{from{transform:scaleX(0)}to{transform:scaleX(1)}}";

  public ScrollProgressBar() {
    this("reading-progress", Tokens.ACCENT_PRIMARY, CssLength.rem(0.25), TimelineAxis.BLOCK);
  }

  public ScrollProgressBar(String id) {
    this(id, Tokens.ACCENT_PRIMARY, CssLength.rem(0.25), TimelineAxis.BLOCK);
  }

  @Override
  public Element render() {
    var el =
        Elements.div()
            .id(id)
            .ariaHidden(true)
            .fixed()
            .insetBlockStart(CssLength.px(0))
            .insetInlineStart(CssLength.px(0))
            .fullWidth()
            .blockSize(thickness)
            .zIndex(50)
            .bg(barColor);

    el.interactivity().registerKeyframeRule(PROGRESS_KEYFRAME);
    el.interactivity()
        .onMotionSafe(
            s ->
                s.put(new CssDeclaration.Animation("tessera-scroll-progress auto linear"))
                    .put(new CssDeclaration.AnimationTimeline("scroll(root " + axis.css() + ")")));
    return el;
  }
}
