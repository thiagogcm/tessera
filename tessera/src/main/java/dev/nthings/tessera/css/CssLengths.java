package dev.nthings.tessera.css;

import dev.nthings.tessera.internal.foundation.Decimals;

/// The [CssLength] implementations, built only through its static factories.
final class CssLengths {

  private CssLengths() {}

  /// The form of `length` used as an operand inside `calc()`. Compound expressions are
  /// parenthesized, so nesting keeps evaluation order.
  static String operand(CssLength length) {
    return switch (length) {
      case Sum sum -> "(" + sum.inner() + ")";
      case Scaled scaled -> "(" + scaled.inner() + ")";
      default -> length.toCss();
    };
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
  }

  /// A number with a unit, such as `12px`.
  record Dimension(double value, Unit unit) implements CssLength {
    @Override
    public String toCss() {
      return Decimals.format(value) + unit.suffix;
    }
  }

  /// The unitless zero and the `auto` keyword.
  enum Keyword implements CssLength {
    ZERO,
    AUTO;

    @Override
    public String toCss() {
      return this == ZERO ? "0" : "auto";
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
    String inner() {
      return operand(left) + (subtract ? " - " : " + ") + operand(right);
    }

    @Override
    public String toCss() {
      return "calc(" + inner() + ")";
    }
  }

  /// `calc(length * factor)` or `calc(length / divisor)`.
  record Scaled(CssLength length, CssNumber factor, boolean divide) implements CssLength {
    String inner() {
      return operand(length) + (divide ? " / " : " * ") + factor.toCss();
    }

    @Override
    public String toCss() {
      return "calc(" + inner() + ")";
    }
  }

  /// `min(first, second)` or `max(first, second)`.
  record Extremum(CssLength first, CssLength second, boolean largest) implements CssLength {
    @Override
    public String toCss() {
      return (largest ? "max(" : "min(") + first.toCss() + ", " + second.toCss() + ")";
    }
  }

  /// `anchor(side)`: an edge of the element's position anchor, for inset properties.
  record Anchor(CssLength.AnchorSide side) implements CssLength {
    @Override
    public String toCss() {
      return "anchor(" + side.toCss() + ")";
    }
  }
}
