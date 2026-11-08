package dev.nthings.tessera.css;

/// A timing function: a keyword or a cubic Bézier curve, used by [Motion].
public sealed interface Easing extends CssValue permits Easing.Preset, Easing.CubicBezier {

  /// Keyword timing functions.
  enum Preset implements Easing, CssKeyword {
    LINEAR,
    EASE,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT
  }

  /// `cubic-bezier(x1, y1, x2, y2)`.
  record CubicBezier(double x1, double y1, double x2, double y2) implements Easing {
    public CubicBezier {
      if (x1 < 0 || x1 > 1 || x2 < 0 || x2 > 1) {
        throw new IllegalArgumentException("Bézier x coordinates must be within [0, 1]");
      }
    }

    @Override
    public String toCss() {
      return "cubic-bezier("
          + CssLength.formatNumber(x1)
          + ", "
          + CssLength.formatNumber(y1)
          + ", "
          + CssLength.formatNumber(x2)
          + ", "
          + CssLength.formatNumber(y2)
          + ")";
    }
  }

  /// `linear`.
  static Easing linear() {
    return Preset.LINEAR;
  }

  /// `ease`.
  static Easing ease() {
    return Preset.EASE;
  }

  /// `ease-in`.
  static Easing easeIn() {
    return Preset.EASE_IN;
  }

  /// `ease-out`.
  static Easing easeOut() {
    return Preset.EASE_OUT;
  }

  /// `ease-in-out`.
  static Easing easeInOut() {
    return Preset.EASE_IN_OUT;
  }

  /// `cubic-bezier(x1, y1, x2, y2)`. Throws IllegalArgumentException if an x coordinate is outside
  /// `[0, 1]`.
  static Easing cubicBezier(double x1, double y1, double x2, double y2) {
    return new CubicBezier(x1, y1, x2, y2);
  }
}
