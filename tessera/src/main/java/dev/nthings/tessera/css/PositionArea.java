package dev.nthings.tessera.css;

import org.jspecify.annotations.Nullable;

/// A `position-area`: where an anchored element sits around its anchor, as one or two keywords.
public record PositionArea(Part first, @Nullable Part second) implements CssValue {

  /// Area keywords, including spans toward one edge.
  public enum Part implements CssKeyword {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    CENTER,
    BLOCK_START,
    BLOCK_END,
    INLINE_START,
    INLINE_END,
    SPAN_TOP,
    SPAN_BOTTOM,
    SPAN_LEFT,
    SPAN_RIGHT,
    SPAN_BLOCK_START,
    SPAN_BLOCK_END,
    SPAN_INLINE_START,
    SPAN_INLINE_END
  }

  public static PositionArea of(Part part) {
    return new PositionArea(part, null);
  }

  public static PositionArea of(Part first, Part second) {
    return new PositionArea(first, second);
  }

  @Override
  public String toCss() {
    return second == null ? first.toCss() : first.toCss() + " " + second.toCss();
  }
}
