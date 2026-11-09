package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.css.Animation;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.Keyframes;
import dev.nthings.tessera.css.TimelineAxis;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Transform;

/// Thin bar fixed to the top of the viewport that fills as the page scrolls. It is decorative
/// and hidden from assistive technology. When the user prefers reduced motion, it stays a
/// static, full-width bar. Use the canonical constructor to change its color, thickness or
/// [TimelineAxis].
public record ScrollProgressBar(
    String id, DesignToken.Color barColor, CssLength thickness, TimelineAxis axis)
    implements Component {

  private static final Keyframes PROGRESS =
      Keyframes.named("tessera-scroll-progress")
          .from(s -> s.transform(Transform.scaleX(0)))
          .to(s -> s.transform(Transform.scaleX(1)));

  /// Creates an accent-colored bar `id`, 0.25rem thick, that follows vertical scrolling in
  /// horizontal text. `id` is local to the enclosing view scope.
  public ScrollProgressBar(String id) {
    this(id, Tokens.ACCENT_PRIMARY, CssLength.rem(0.25), TimelineAxis.BLOCK);
  }

  @Override
  public Element render(RenderContext context) {
    return Elements.div()
        .id(ElementRef.scoped(id))
        .ariaHidden(true)
        .fixed()
        .insetBlockStart(CssLength.px(0))
        .insetInlineStart(CssLength.px(0))
        .inlineSize(CssLength.pct(100))
        .blockSize(thickness)
        .zIndex(50)
        .bg(barColor)
        .animation(Animation.scrollProgress(PROGRESS).along(axis));
  }
}
