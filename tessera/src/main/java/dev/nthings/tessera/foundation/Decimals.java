package dev.nthings.tessera.foundation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/// Compiler-facing: formats numbers for CSS values and ARIA attributes.
public final class Decimals {

  private Decimals() {}

  /// `value` in plain notation with at most three fraction digits (rounded half-even) and no
  /// trailing zeros, such as `0.5`, `12` or `-0.125`.
  public static String format(double value) {
    return new BigDecimal(value)
        .setScale(3, RoundingMode.HALF_EVEN)
        .stripTrailingZeros()
        .toPlainString();
  }
}
