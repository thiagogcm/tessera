package dev.nthings.tessera.css;

/// A flex layout preset, applied with [LayoutStyled#layout(FlexLayout)]. The preset is copied into
/// the element's [StyleRefinement], so a later declaration replaces only that property. Block and
/// inline layouts are separate choices.
public record FlexLayout(
    Display display, Direction direction, CssLength gap, Align align, Wrap wrap) {

  /// Block flex with `flex-direction: row`.
  public static FlexLayout row(CssLength gap, Align align, Wrap wrap) {
    return new FlexLayout(Display.FLEX, Direction.ROW, gap, align, wrap);
  }

  /// Block flex with `flex-direction: column`.
  public static FlexLayout column(CssLength gap, Align align, Wrap wrap) {
    return new FlexLayout(Display.FLEX, Direction.COLUMN, gap, align, wrap);
  }

  /// Inline flex with `flex-direction: row`. The container stays on the surrounding line.
  public static FlexLayout inlineRow(CssLength gap, Align align, Wrap wrap) {
    return new FlexLayout(Display.INLINE_FLEX, Direction.ROW, gap, align, wrap);
  }

  /// Inline flex with `flex-direction: column`. The container stays on the surrounding line.
  public static FlexLayout inlineColumn(CssLength gap, Align align, Wrap wrap) {
    return new FlexLayout(Display.INLINE_FLEX, Direction.COLUMN, gap, align, wrap);
  }

  /// Writes this layout's declarations into `style`; [LayoutStyled#layout(FlexLayout)] calls it.
  public StyleRefinement apply(StyleRefinement style) {
    return style
        .put(CssProperty.DISPLAY, display)
        .put(CssProperty.FLEX_DIRECTION, direction)
        .put(CssProperty.FLEX_WRAP, wrap)
        .put(CssProperty.ALIGN_ITEMS, align)
        .put(CssProperty.GAP, gap);
  }

  /// The `display` of a flex layout: block or inline flex.
  public enum Display implements CssKeyword {
    FLEX,
    INLINE_FLEX
  }

  /// The `flex-direction` of a flex layout.
  public enum Direction implements CssKeyword {
    ROW,
    COLUMN
  }

  /// Cross-axis alignment (`align-items`). `START` and `END` emit `flex-start` and `flex-end`.
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

  /// The `flex-wrap` of a flex layout.
  public enum Wrap implements CssKeyword {
    NOWRAP,
    WRAP
  }
}
