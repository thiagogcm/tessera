package dev.nthings.tessera.css;

import dev.nthings.tessera.internal.foundation.Decimals;
import java.util.HexFormat;
import java.util.Optional;

/// The [CssColor] implementations, built only through its factories and derivation methods.
final class CssColors {

  private CssColors() {}

  static Optional<CssColor> parseHex(String hex) {
    if (!hex.startsWith("#") || !hex.chars().skip(1).allMatch(HexFormat::isHexDigit)) {
      return Optional.empty();
    }
    var digits = hex.substring(1);
    if (digits.length() <= 4) {
      var expanded = new StringBuilder(digits.length() * 2);
      digits.chars().forEach(c -> expanded.append((char) c).append((char) c));
      digits = expanded.toString();
    }
    return switch (digits.length()) {
      case 6 -> Optional.of(new Srgb(HexFormat.fromHexDigits(digits)));
      case 8 -> {
        var rgba = HexFormat.fromHexDigits(digits);
        yield Optional.of(new Srgb(rgba >>> 8, (rgba & 0xff) / 255.0));
      }
      default -> Optional.empty();
    };
  }

  private static void requireAlpha(double alpha) {
    if (!(alpha >= 0 && alpha <= 1)) {
      throw new IllegalArgumentException("Alpha must be within [0, 1]: " + alpha);
    }
  }

  /// `oklch(L% C H)` or, below full opacity, `oklch(L% C H / A)`. Lightness is a percentage.
  record Oklch(double lightness, double chroma, double hue, double alpha) implements CssColor {
    Oklch {
      requireAlpha(alpha);
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

  /// `#rrggbb` or, below full opacity, `rgb(R G B / A)`.
  record Srgb(int rgb, double alpha) implements CssColor {
    Srgb {
      if (rgb < 0 || rgb > 0xffffff) {
        throw new IllegalArgumentException("Not a 24-bit color: " + rgb);
      }
      requireAlpha(alpha);
    }

    Srgb(int rgb) {
      this(rgb, 1);
    }

    @Override
    public String toCss() {
      if (alpha < 1) {
        var r = (rgb >>> 16) & 0xff;
        var g = (rgb >>> 8) & 0xff;
        var b = rgb & 0xff;
        return "rgb(" + r + " " + g + " " + b + " / " + Decimals.format(alpha) + ")";
      }
      return "#" + HexFormat.of().toHexDigits(rgb).substring(2);
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
