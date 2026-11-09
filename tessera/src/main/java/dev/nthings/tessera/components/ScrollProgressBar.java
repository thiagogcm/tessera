package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.css.CssIdent;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.InteractiveState;
import dev.nthings.tessera.css.TimelineAxis;
import dev.nthings.tessera.css.Tokens;

/// Thin bar fixed to the top of the viewport that fills as the page scrolls. It is decorative
/// and hidden from assistive technology. When the user prefers reduced motion, it stays a
/// static, full-width bar. Use the canonical constructor to change its color, thickness or
/// [TimelineAxis].
public record ScrollProgressBar(
    String id, DesignToken.Color barColor, CssLength thickness, TimelineAxis axis)
    implements Component {

  private static final String PROGRESS_KEYFRAME =
      "@keyframes tessera-scroll-progress{from{transform:scaleX(0)}to{transform:scaleX(1)}}";

  /// Creates an accent-colored bar `id`, 0.25rem thick, that follows vertical scrolling in
  /// horizontal text. `id` is local to the enclosing view scope.
  public ScrollProgressBar(String id) {
    this(id, Tokens.ACCENT_PRIMARY, CssLength.rem(0.25), TimelineAxis.BLOCK);
  }

  @Override
  public Element render(RenderContext context) {
    var el =
        Elements.div()
            .id(ElementRef.scoped(id))
            .ariaHidden(true)
            .fixed()
            .insetBlockStart(CssLength.px(0))
            .insetInlineStart(CssLength.px(0))
            .inlineSize(CssLength.pct(100))
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
