package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.css.CssIdent;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.InteractiveState;
import dev.nthings.tessera.css.TimelineAxis;
import dev.nthings.tessera.css.Tokens;

/// Scroll-driven progress bar (`animation-timeline: scroll(root <axis>)`), gated behind
/// `@media (prefers-reduced-motion: no-preference)`.
///
/// Emits `aria-hidden="true"`, since the indicator is decorative.
public record ScrollProgressBar(
    String id, DesignToken.Color barColor, CssLength thickness, TimelineAxis axis)
    implements RenderOnce {

  private static final String PROGRESS_KEYFRAME =
      "@keyframes tessera-scroll-progress{from{transform:scaleX(0)}to{transform:scaleX(1)}}";

  public ScrollProgressBar(String id) {
    this(id, Tokens.ACCENT_PRIMARY, CssLength.rem(0.25), TimelineAxis.BLOCK);
  }

  @Override
  public Element render(RenderContext context) {
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
        .on(
            InteractiveState.MOTION_SAFE,
            s -> s.scrollProgressAnimation(CssIdent.of("tessera-scroll-progress"), axis));
    return el;
  }
}
