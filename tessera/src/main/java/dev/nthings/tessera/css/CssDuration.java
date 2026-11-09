package dev.nthings.tessera.css;

/// A CSS time: milliseconds written inline, or a design token. Used by [Motion]. Build values with
/// [#ms(double)].
public sealed interface CssDuration extends CssValue
    permits CssDurations.Millis, DesignToken.Duration {

  /// `<millis>ms`, such as `150ms`. Throws IllegalArgumentException if `millis` is negative.
  static CssDuration ms(double millis) {
    return new CssDurations.Millis(millis);
  }
}
