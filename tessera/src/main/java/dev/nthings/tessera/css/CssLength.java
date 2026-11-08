package dev.nthings.tessera.css;

import java.math.BigDecimal;
import java.math.RoundingMode;

/// A CSS length: a dimension (`px`, `rem`, ...), `0`, `auto`, `clamp()` from [#fluid], `min()` or
/// `max()`, `calc()` from [#plus], [#minus], [#times], and [#dividedBy], `anchor()` from [#anchor],
/// or a length token ([DesignToken.Length]).
public sealed interface CssLength extends CssValue
    permits CssLength.Dimension,
        CssLength.Zero,
        CssLength.Auto,
        CssLength.Fluid,
        CssLength.Sum,
        CssLength.Scaled,
        CssLength.Extremum,
        CssLength.Anchor,
        DesignToken.Length {

  /// Compiler-facing: formats `value` for CSS, with at most three fraction digits.
  static String formatNumber(double value) {
    return new BigDecimal(value)
        .setScale(3, RoundingMode.HALF_EVEN)
        .stripTrailingZeros()
        .toPlainString();
  }

  /// Compiler-facing: the form used as an operand inside `calc()`. Compound expressions are
  /// parenthesized, so nesting keeps evaluation order.
  default String operand() {
    return toCss();
  }

  /// `calc(this + other)`.
  default CssLength plus(CssLength other) {
    return new Sum(this, other, false);
  }

  /// `calc(this - other)`.
  default CssLength minus(CssLength other) {
    return new Sum(this, other, true);
  }

  /// `calc(this * factor)`.
  default CssLength times(CssNumber factor) {
    return new Scaled(this, factor, false);
  }

  /// `calc(this / divisor)`.
  default CssLength dividedBy(CssNumber divisor) {
    return new Scaled(this, divisor, true);
  }

  /// CSS length units and their suffixes.
  enum Unit {
    PX("px"),
    REM("rem"),
    EM("em"),
    CH("ch"),
    LH("lh"),
    PERCENT("%"),
    CQI("cqi"),
    DVH("dvh"),
    DVW("dvw");

    private final String suffix;

    Unit(String suffix) {
      this.suffix = suffix;
    }

    public String suffix() {
      return suffix;
    }
  }

  /// A number with a unit, such as `12px`.
  record Dimension(double value, Unit unit) implements CssLength {
    @Override
    public String toCss() {
      return formatNumber(value) + unit.suffix();
    }
  }

  /// The unitless zero, valid wherever a length is.
  enum Zero implements CssLength {
    INSTANCE;

    @Override
    public String toCss() {
      return "0";
    }
  }

  /// The `auto` keyword.
  enum Auto implements CssLength {
    INSTANCE;

    @Override
    public String toCss() {
      return "auto";
    }
  }

  /// `clamp(min, preferred, max)`.
  record Fluid(CssLength min, CssLength preferred, CssLength max) implements CssLength {
    @Override
    public String toCss() {
      return "clamp(" + min.toCss() + ", " + preferred.toCss() + ", " + max.toCss() + ")";
    }
  }

  /// `calc(left + right)` or `calc(left - right)`.
  record Sum(CssLength left, CssLength right, boolean subtract) implements CssLength {
    private String inner() {
      return left.operand() + (subtract ? " - " : " + ") + right.operand();
    }

    @Override
    public String toCss() {
      return "calc(" + inner() + ")";
    }

    @Override
    public String operand() {
      return "(" + inner() + ")";
    }
  }

  /// `calc(length * factor)` or `calc(length / divisor)`.
  record Scaled(CssLength length, CssNumber factor, boolean divide) implements CssLength {
    private String inner() {
      return length.operand() + (divide ? " / " : " * ") + factor.toCss();
    }

    @Override
    public String toCss() {
      return "calc(" + inner() + ")";
    }

    @Override
    public String operand() {
      return "(" + inner() + ")";
    }
  }

  /// `min(first, second)` or `max(first, second)`.
  record Extremum(CssLength first, CssLength second, boolean largest) implements CssLength {
    @Override
    public String toCss() {
      return (largest ? "max(" : "min(") + first.toCss() + ", " + second.toCss() + ")";
    }
  }

  /// Edge of the position anchor that `anchor()` resolves to.
  enum AnchorSide implements CssKeyword {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    CENTER
  }

  /// `anchor(side)`: an edge of the element's position anchor, for inset properties.
  record Anchor(AnchorSide side) implements CssLength {
    @Override
    public String toCss() {
      return "anchor(" + side.toCss() + ")";
    }
  }

  /// A length in pixels, such as `12px`.
  static CssLength px(double value) {
    return new Dimension(value, Unit.PX);
  }

  /// A length in root font sizes, such as `1rem`.
  static CssLength rem(double value) {
    return new Dimension(value, Unit.REM);
  }

  /// A length in the element's font size, such as `1em`.
  static CssLength em(double value) {
    return new Dimension(value, Unit.EM);
  }

  /// A length in the width of the `0` glyph, such as `2ch`.
  static CssLength ch(double value) {
    return new Dimension(value, Unit.CH);
  }

  /// A multiple of the element's computed line height, such as `1.5lh`.
  static CssLength lh(double value) {
    return new Dimension(value, Unit.LH);
  }

  /// A percentage, such as `50%`.
  static CssLength pct(double value) {
    return new Dimension(value, Unit.PERCENT);
  }

  /// A percentage of the nearest query container's inline size, such as `50cqi`.
  static CssLength cqi(double value) {
    return new Dimension(value, Unit.CQI);
  }

  /// A percentage of the dynamic viewport height, such as `100dvh`.
  static CssLength dvh(double value) {
    return new Dimension(value, Unit.DVH);
  }

  /// A percentage of the dynamic viewport width, such as `100dvw`.
  static CssLength dvw(double value) {
    return new Dimension(value, Unit.DVW);
  }

  /// `0`, valid wherever a length is.
  static CssLength zero() {
    return Zero.INSTANCE;
  }

  /// `auto`. Not valid for padding or gap; the methods that take those do not reject it.
  static CssLength auto() {
    return Auto.INSTANCE;
  }

  /// `clamp(min, preferred, max)`: `preferred`, kept between `min` and `max`.
  static CssLength fluid(CssLength min, CssLength preferred, CssLength max) {
    return new Fluid(min, preferred, max);
  }

  /// `min(first, second)`.
  static CssLength min(CssLength first, CssLength second) {
    return new Extremum(first, second, false);
  }

  /// `max(first, second)`.
  static CssLength max(CssLength first, CssLength second) {
    return new Extremum(first, second, true);
  }

  /// `anchor(side)`: an edge of the position anchor, for inset properties such as
  /// [LayoutStyled#insetBlockStart(CssLength)].
  static CssLength anchor(AnchorSide side) {
    return new Anchor(side);
  }
}
