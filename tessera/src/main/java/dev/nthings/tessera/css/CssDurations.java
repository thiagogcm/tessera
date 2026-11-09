package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.Decimals;

/// The [CssDuration] implementations, built only through [CssDuration#ms(double)].
final class CssDurations {

  private CssDurations() {}

  /// A literal time in milliseconds.
  record Millis(double millis) implements CssDuration {
    Millis {
      if (millis < 0) {
        throw new IllegalArgumentException("Duration cannot be negative: " + millis);
      }
    }

    @Override
    public String toCss() {
      return Decimals.format(millis) + "ms";
    }
  }
}
