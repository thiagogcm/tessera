package dev.nthings.tessera.css;

import java.math.BigDecimal;
import java.math.RoundingMode;

/// Sealed algebraic CSS lengths: dimensions, math functions over lengths, anchor references, and
/// design tokens.
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

  /// Formats `value` with at most three fraction digits (half-even) and no trailing zeros.
  static String formatNumber(double value) {
    return new BigDecimal(value)
        .setScale(3, RoundingMode.HALF_EVEN)
        .stripTrailingZeros()
        .toPlainString();
  }

  /// This length as an operand of a surrounding math expression. Compound expressions are
  /// parenthesized, so nesting never changes evaluation order.
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

  /// Numeric length in a CSS unit.
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

  enum Auto implements CssLength {
    INSTANCE;

    @Override
    public String toCss() {
      return "auto";
    }
  }

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

  /// Which edge of the position anchor an `anchor()` length resolves to.
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

  static CssLength px(double value) {
    return new Dimension(value, Unit.PX);
  }

  static CssLength rem(double value) {
    return new Dimension(value, Unit.REM);
  }

  static CssLength em(double value) {
    return new Dimension(value, Unit.EM);
  }

  static CssLength ch(double value) {
    return new Dimension(value, Unit.CH);
  }

  /// A multiple of the element's computed line height.
  static CssLength lh(double value) {
    return new Dimension(value, Unit.LH);
  }

  static CssLength pct(double value) {
    return new Dimension(value, Unit.PERCENT);
  }

  static CssLength cqi(double value) {
    return new Dimension(value, Unit.CQI);
  }

  static CssLength dvh(double value) {
    return new Dimension(value, Unit.DVH);
  }

  static CssLength dvw(double value) {
    return new Dimension(value, Unit.DVW);
  }

  static CssLength zero() {
    return Zero.INSTANCE;
  }

  static CssLength auto() {
    return Auto.INSTANCE;
  }

  static CssLength fluid(CssLength min, CssLength preferred, CssLength max) {
    return new Fluid(min, preferred, max);
  }

  static CssLength min(CssLength first, CssLength second) {
    return new Extremum(first, second, false);
  }

  static CssLength max(CssLength first, CssLength second) {
    return new Extremum(first, second, true);
  }

  static CssLength anchor(AnchorSide side) {
    return new Anchor(side);
  }
}
