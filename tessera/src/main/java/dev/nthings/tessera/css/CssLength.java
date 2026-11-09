package dev.nthings.tessera.css;

import com.google.errorprone.annotations.CheckReturnValue;

/// A CSS length: a dimension (`px`, `rem`, ...), `0`, `auto`, `clamp()` from [#fluid], `min()` or
/// `max()`, `calc()` from [#plus], [#minus], [#times], and [#dividedBy], `anchor()` from [#anchor],
/// or a length token ([DesignToken.Length]). Build values with the static factories.
@CheckReturnValue
public sealed interface CssLength extends CssValue
    permits CssLengths.Dimension,
        CssLengths.Keyword,
        CssLengths.Fluid,
        CssLengths.Sum,
        CssLengths.Scaled,
        CssLengths.Extremum,
        CssLengths.Anchor,
        DesignToken.Length {

  /// `calc(this + other)`.
  default CssLength plus(CssLength other) {
    return new CssLengths.Sum(this, other, false);
  }

  /// `calc(this - other)`.
  default CssLength minus(CssLength other) {
    return new CssLengths.Sum(this, other, true);
  }

  /// `calc(this * factor)`.
  default CssLength times(CssNumber factor) {
    return new CssLengths.Scaled(this, factor, false);
  }

  /// `calc(this / divisor)`.
  default CssLength dividedBy(CssNumber divisor) {
    return new CssLengths.Scaled(this, divisor, true);
  }

  /// Edge of the position anchor that `anchor()` resolves to.
  enum AnchorSide implements CssKeyword {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    CENTER
  }

  /// A length in pixels, such as `12px`.
  static CssLength px(double value) {
    return new CssLengths.Dimension(value, CssLengths.Unit.PX);
  }

  /// A length in root font sizes, such as `1rem`.
  static CssLength rem(double value) {
    return new CssLengths.Dimension(value, CssLengths.Unit.REM);
  }

  /// A length in the element's font size, such as `1em`.
  static CssLength em(double value) {
    return new CssLengths.Dimension(value, CssLengths.Unit.EM);
  }

  /// A length in the width of the `0` glyph, such as `2ch`.
  static CssLength ch(double value) {
    return new CssLengths.Dimension(value, CssLengths.Unit.CH);
  }

  /// A multiple of the element's computed line height, such as `1.5lh`.
  static CssLength lh(double value) {
    return new CssLengths.Dimension(value, CssLengths.Unit.LH);
  }

  /// A percentage, such as `50%`.
  static CssLength pct(double value) {
    return new CssLengths.Dimension(value, CssLengths.Unit.PERCENT);
  }

  /// A percentage of the nearest query container's inline size, such as `50cqi`.
  static CssLength cqi(double value) {
    return new CssLengths.Dimension(value, CssLengths.Unit.CQI);
  }

  /// A percentage of the dynamic viewport height, such as `100dvh`.
  static CssLength dvh(double value) {
    return new CssLengths.Dimension(value, CssLengths.Unit.DVH);
  }

  /// A percentage of the dynamic viewport width, such as `100dvw`.
  static CssLength dvw(double value) {
    return new CssLengths.Dimension(value, CssLengths.Unit.DVW);
  }

  /// `0`, valid wherever a length is.
  static CssLength zero() {
    return CssLengths.Keyword.ZERO;
  }

  /// `auto`. Not valid for padding or gap; the methods that take those do not reject it.
  static CssLength auto() {
    return CssLengths.Keyword.AUTO;
  }

  /// `clamp(min, preferred, max)`: `preferred`, kept between `min` and `max`.
  static CssLength fluid(CssLength min, CssLength preferred, CssLength max) {
    return new CssLengths.Fluid(min, preferred, max);
  }

  /// `min(first, second)`.
  static CssLength min(CssLength first, CssLength second) {
    return new CssLengths.Extremum(first, second, false);
  }

  /// `max(first, second)`.
  static CssLength max(CssLength first, CssLength second) {
    return new CssLengths.Extremum(first, second, true);
  }

  /// `anchor(side)`: an edge of the position anchor, for inset properties such as
  /// [Styled#insetBlockStart(CssLength)].
  static CssLength anchor(AnchorSide side) {
    return new CssLengths.Anchor(side);
  }
}
