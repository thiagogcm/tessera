package dev.nthings.tessera.css;

/// A complete flex formatting context: display, direction, gap, item alignment, and wrapping.
///
/// Apply it with [Styled#layout(FlexLayout)]. The value is copied into the element's
/// [StyleRefinement], so a later declaration replaces that one property on that element. Block
/// (`row`, `column`) and inline (`inlineRow`, `inlineColumn`) layouts are separate choices.
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

  /// Copies these declarations into `style`.
  public StyleRefinement apply(StyleRefinement style) {
    return style
        .put(CssProperty.DISPLAY, display)
        .put(CssProperty.FLEX_DIRECTION, direction)
        .put(CssProperty.FLEX_WRAP, wrap)
        .put(CssProperty.ALIGN_ITEMS, align)
        .put(CssProperty.GAP, gap);
  }

  /// `display` value. Block flex and inline flex are explicit.
  public enum Display implements CssKeyword {
    FLEX,
    INLINE_FLEX
  }

  /// `flex-direction` value.
  public enum Direction implements CssKeyword {
    ROW,
    COLUMN
  }

  /// `align-items` value.
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

  /// `flex-wrap` value.
  public enum Wrap implements CssKeyword {
    NOWRAP,
    WRAP
  }
}
