package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.Decimals;

/// The [CssColor] implementations, built only through its factories and derivation methods.
final class CssColors {

  private CssColors() {}

  /// `oklch(L% C H)` or, below full opacity, `oklch(L% C H / A)`. Lightness is a percentage.
  record Oklch(double lightness, double chroma, double hue, double alpha) implements CssColor {
    Oklch {
      if (alpha < 0 || alpha > 1) {
        throw new IllegalArgumentException("Alpha must be within [0, 1]: " + alpha);
      }
    }

    @Override
    public String toCss() {
      var base =
          "oklch("
              + Decimals.format(lightness)
              + "% "
              + Decimals.format(chroma)
              + " "
              + Decimals.format(hue);
      return alpha < 1 ? base + " / " + Decimals.format(alpha) + ")" : base + ")";
    }
  }

  /// `#rrggbb`.
  record Srgb(int rgb) implements CssColor {
    Srgb {
      if (rgb < 0 || rgb > 0xffffff) {
        throw new IllegalArgumentException("Not a 24-bit color: " + rgb);
      }
    }

    @Override
    public String toCss() {
      return String.format("#%06x", rgb);
    }
  }

  /// The keywords `transparent` and `currentColor`.
  enum Named implements CssColor, CssKeyword {
    TRANSPARENT,
    CURRENT_COLOR;

    @Override
    public String toCss() {
      return this == CURRENT_COLOR ? "currentColor" : CssKeyword.super.toCss();
    }
  }

  /// `color-mix(in <space>, first <weight>, second)`; `weight` is the share of `first`.
  record Mix(CssColor.Space space, CssColor first, CssLength weight, CssColor second)
      implements CssColor {
    @Override
    public String toCss() {
      return "color-mix(in "
          + space.toCss()
          + ", "
          + first.toCss()
          + " "
          + weight.toCss()
          + ", "
          + second.toCss()
          + ")";
    }
  }

  /// `oklch(from base L% C h)`: the base color's hue at a fixed lightness and chroma.
  record Relative(CssColor base, double lightness, double chroma) implements CssColor {
    @Override
    public String toCss() {
      return "oklch(from "
          + base.toCss()
          + " "
          + Decimals.format(lightness)
          + "% "
          + Decimals.format(chroma)
          + " h)";
    }
  }

  /// `light-dark(light, dark)`.
  record LightDark(CssColor light, CssColor dark) implements CssColor {
    @Override
    public String toCss() {
      return "light-dark(" + light.toCss() + ", " + dark.toCss() + ")";
    }
  }

  /// `contrast-color(background)`: black or white, whichever reads better on `background`.
  record ContrastOn(CssColor background) implements CssColor {
    @Override
    public String toCss() {
      return "contrast-color(" + background.toCss() + ")";
    }
  }
}
