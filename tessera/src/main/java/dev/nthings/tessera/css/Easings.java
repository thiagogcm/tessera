package dev.nthings.tessera.css;

import dev.nthings.tessera.internal.foundation.Decimals;

/// The [Easing] implementations, built only through its static factories.
final class Easings {

  private Easings() {}

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
    CubicBezier {
      if (x1 < 0 || x1 > 1 || x2 < 0 || x2 > 1) {
        throw new IllegalArgumentException("Bézier x coordinates must be within [0, 1]");
      }
    }

    @Override
    public String toCss() {
      return "cubic-bezier("
          + Decimals.format(x1)
          + ", "
          + Decimals.format(y1)
          + ", "
          + Decimals.format(x2)
          + ", "
          + Decimals.format(y2)
          + ")";
    }
  }
}
