package dev.nthings.tessera.css;

/// A timing function: a keyword or a cubic Bézier curve, used by [Transition] and [Animation].
/// Build values with the static factories.
public sealed interface Easing extends CssValue permits Easings.Preset, Easings.CubicBezier {

  /// `linear`.
  static Easing linear() {
    return Easings.Preset.LINEAR;
  }

  /// `ease`.
  static Easing ease() {
    return Easings.Preset.EASE;
  }

  /// `ease-in`.
  static Easing easeIn() {
    return Easings.Preset.EASE_IN;
  }

  /// `ease-out`.
  static Easing easeOut() {
    return Easings.Preset.EASE_OUT;
  }

  /// `ease-in-out`.
  static Easing easeInOut() {
    return Easings.Preset.EASE_IN_OUT;
  }

  /// `cubic-bezier(x1, y1, x2, y2)`. Throws IllegalArgumentException if an x coordinate is outside
  /// `[0, 1]`.
  static Easing cubicBezier(double x1, double y1, double x2, double y2) {
    return new Easings.CubicBezier(x1, y1, x2, y2);
  }
}
