package dev.nthings.tessera.css;

import java.math.BigDecimal;
import java.math.RoundingMode;

/// Sealed algebraic CSS length units.
public sealed interface CssLength
    permits CssLength.Dimension, CssLength.Auto, CssLength.Fluid, CssLength.Expr {

  String toCss();

  /// Formats `value` with at most three fraction digits (half-even) and no trailing zeros.
  static String formatNumber(double value) {
    return new BigDecimal(value)
        .setScale(3, RoundingMode.HALF_EVEN)
        .stripTrailingZeros()
        .toPlainString();
  }

  /// Numeric length in a CSS unit.
  enum Unit {
    PX("px"),
    REM("rem"),
    EM("em"),
    CH("ch"),
    PERCENT("%"),
    CQI("cqi"),
    DVH("dvh");

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
      return "--fluid(" + min.toCss() + ", " + preferred.toCss() + ", " + max.toCss() + ")";
    }
  }

  record Expr(String expression) implements CssLength {
    @Override
    public String toCss() {
      return expression;
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

  static CssLength pct(double value) {
    return new Dimension(value, Unit.PERCENT);
  }

  static CssLength cqi(double value) {
    return new Dimension(value, Unit.CQI);
  }

  static CssLength dvh(double value) {
    return new Dimension(value, Unit.DVH);
  }

  static CssLength auto() {
    return Auto.INSTANCE;
  }

  static CssLength fluid(CssLength min, CssLength preferred, CssLength max) {
    return new Fluid(min, preferred, max);
  }

  static CssLength expr(String expression) {
    return new Expr(expression);
  }
}
