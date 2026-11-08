package dev.nthings.tessera.css;

/// Apply with [Styled#layout(FlexLayout)]. Copied into the element's [StyleRefinement], so a later
/// declaration replaces only that property. Block and inline layouts are separate choices.
public record FlexLayout(
    Display display, Direction direction, CssLength gap, Align align, Wrap wrap) {

  /// Block flex, along the inline axis.
  public static FlexLayout row(CssLength gap, Align align, Wrap wrap) {
    return new FlexLayout(Display.FLEX, Direction.ROW, gap, align, wrap);
  }

  /// Block flex, along the block axis.
  public static FlexLayout column(CssLength gap, Align align, Wrap wrap) {
    return new FlexLayout(Display.FLEX, Direction.COLUMN, gap, align, wrap);
  }

  /// Inline flex, along the inline axis. The container stays on the surrounding line.
  public static FlexLayout inlineRow(CssLength gap, Align align, Wrap wrap) {
    return new FlexLayout(Display.INLINE_FLEX, Direction.ROW, gap, align, wrap);
  }

  /// Inline flex, along the block axis. The container stays on the surrounding line.
  public static FlexLayout inlineColumn(CssLength gap, Align align, Wrap wrap) {
    return new FlexLayout(Display.INLINE_FLEX, Direction.COLUMN, gap, align, wrap);
  }

  public StyleRefinement apply(StyleRefinement style) {
    return style
        .put(CssProperty.DISPLAY, display)
        .put(CssProperty.FLEX_DIRECTION, direction)
        .put(CssProperty.FLEX_WRAP, wrap)
        .put(CssProperty.ALIGN_ITEMS, align)
        .put(CssProperty.GAP, gap);
  }

  public enum Display implements CssKeyword {
    FLEX,
    INLINE_FLEX
  }

  public enum Direction implements CssKeyword {
    ROW,
    COLUMN
  }

  public enum Align implements CssKeyword {
    START,
    CENTER,
    END,
    STRETCH,
    BASELINE;

    @Override
    public String toCss() {
      return switch (this) {
        case START -> "flex-start";
        case END -> "flex-end";
        default -> CssKeyword.super.toCss();
      };
    }
  }

  public enum Wrap implements CssKeyword {
    NOWRAP,
    WRAP
  }
}
