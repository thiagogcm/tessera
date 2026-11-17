package dev.nthings.tessera.components;

import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.Animation;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.Keyframes;
import dev.nthings.tessera.css.TimelineAxis;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Transform;

/// Thin bar fixed to the top of the viewport that fills as the page scrolls. It is decorative and
/// hidden from assistive technology. When the user prefers reduced motion, it stays a static,
/// full-width bar. Create one with [#of()]; it is accent-colored, 0.25rem thick, and follows
/// block-axis scrolling unless changed.
public final class ScrollProgressBar extends BuiltinComponent {

  private static final Keyframes PROGRESS =
      Keyframes.named("tessera-scroll-progress")
          .start(s -> s.transform(Transform.scaleX(0)))
          .end(s -> s.transform(Transform.scaleX(1)));

  private DesignToken.Color color = Tokens.ACCENT_PRIMARY;
  private CssLength thickness = CssLength.rem(0.25);
  private TimelineAxis axis = TimelineAxis.BLOCK;

  private ScrollProgressBar() {}

  /// Creates an accent-colored bar, 0.25rem thick, that follows vertical scrolling in horizontal
  /// text.
  public static ScrollProgressBar of() {
    return new ScrollProgressBar();
  }

  /// Sets the bar color. The default is [Tokens#ACCENT_PRIMARY].
  public ScrollProgressBar color(DesignToken.Color color) {
    this.color = color;
    return this;
  }

  /// Sets the bar thickness. The default is 0.25rem.
  public ScrollProgressBar thickness(CssLength thickness) {
    this.thickness = thickness;
    return this;
  }

  /// Sets the scroll axis the bar follows. The default is `BLOCK`.
  public ScrollProgressBar axis(TimelineAxis axis) {
    this.axis = axis;
    return this;
  }

  @Override
  IntoElement expand(DocumentContext context) {
    return Elements.div()
        .ariaHidden(true)
        .fixed()
        .insetBlockStart(CssLength.px(0))
        .insetInlineStart(CssLength.px(0))
        .inlineSize(CssLength.pct(100))
        .blockSize(thickness)
        .zIndex(50)
        .bg(color)
        .animation(Animation.scrollProgress(PROGRESS).along(axis));
  }
}
