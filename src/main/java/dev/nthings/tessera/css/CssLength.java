package dev.nthings.tessera.css;

import java.math.BigDecimal;
import java.math.RoundingMode;

/// Sealed algebraic CSS length units.
public sealed interface CssLength
    permits CssLength.Px,
        CssLength.Rem,
        CssLength.Em,
        CssLength.Ch,
        CssLength.Percent,
        CssLength.Cqi,
        CssLength.Dvh,
        CssLength.Auto,
        CssLength.Fluid,
        CssLength.Expr {

  String toCss();

  /// Formats `value` with at most three fraction digits (half-even) and no trailing zeros.
  static String formatNumber(double value) {
    return new BigDecimal(value)
        .setScale(3, RoundingMode.HALF_EVEN)
        .stripTrailingZeros()
        .toPlainString();
  }

  record Px(double value) implements CssLength {
    @Override
    public String toCss() {
      return formatNumber(value) + "px";
    }
  }

  record Rem(double value) implements CssLength {
    @Override
    public String toCss() {
      return formatNumber(value) + "rem";
    }
  }

  record Em(double value) implements CssLength {
    @Override
    public String toCss() {
      return formatNumber(value) + "em";
    }
  }

  record Ch(double value) implements CssLength {
    @Override
    public String toCss() {
      return formatNumber(value) + "ch";
    }
  }

  record Percent(double value) implements CssLength {
    @Override
    public String toCss() {
      return formatNumber(value) + "%";
    }
  }

  record Cqi(double value) implements CssLength {
    @Override
    public String toCss() {
      return formatNumber(value) + "cqi";
    }
  }

  record Dvh(double value) implements CssLength {
    @Override
    public String toCss() {
      return formatNumber(value) + "dvh";
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
    return new Px(value);
  }

  static CssLength rem(double value) {
    return new Rem(value);
  }

  static CssLength em(double value) {
    return new Em(value);
  }

  static CssLength ch(double value) {
    return new Ch(value);
  }

  static CssLength pct(double value) {
    return new Percent(value);
  }

  static CssLength cqi(double value) {
    return new Cqi(value);
  }

  static CssLength dvh(double value) {
    return new Dvh(value);
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
