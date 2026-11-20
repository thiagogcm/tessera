package dev.nthings.tessera.css;

import java.time.Duration;

/// A CSS time: milliseconds written inline, or a design token. Used by [Transition] and
/// [Animation]. Build values with [#ms(double)] or [#of(Duration)].
public sealed interface CssDuration extends CssValue
    permits CssDurations.Millis, DesignToken.Duration {

  /// `<millis>ms`, such as `150ms`. Throws IllegalArgumentException if `millis` is negative.
  static CssDuration ms(double millis) {
    return new CssDurations.Millis(millis);
  }

  /// Converts a non-negative [Duration] into CSS milliseconds. Throws IllegalArgumentException if
  /// `duration` is negative.
  static CssDuration of(Duration duration) {
    if (duration.isNegative()) {
      throw new IllegalArgumentException("Duration must not be negative: " + duration);
    }
    return ms(duration.toNanos() / 1_000_000.0);
  }
}
