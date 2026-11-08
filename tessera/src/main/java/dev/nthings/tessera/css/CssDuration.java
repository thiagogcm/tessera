package dev.nthings.tessera.css;

/// A CSS time: milliseconds written inline, or a design token.
public sealed interface CssDuration extends CssValue
    permits CssDuration.Time, DesignToken.Duration {

  record Time(double millis) implements CssDuration {
    public Time {
      if (millis < 0) {
        throw new IllegalArgumentException("Duration cannot be negative: " + millis);
      }
    }

    @Override
    public String toCss() {
      return CssLength.formatNumber(millis) + "ms";
    }
  }

  static CssDuration ms(double millis) {
    return new Time(millis);
  }
}
