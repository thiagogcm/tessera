package dev.nthings.tessera.css;

/// A CSS color: OKLCH and sRGB literals, keywords, derived colors (`color-mix`, relative
/// `oklch(from ...)`, `light-dark`, `contrast-color`), and design tokens.
public sealed interface CssColor extends CssValue
    permits CssColor.Oklch,
        CssColor.Srgb,
        CssColor.Named,
        CssColor.Mix,
        CssColor.Relative,
        CssColor.LightDark,
        CssColor.ContrastOn,
        DesignToken.Color {

  /// `oklch(L% C H)` or, below full opacity, `oklch(L% C H / A)`. Lightness is a percentage.
  record Oklch(double lightness, double chroma, double hue, double alpha) implements CssColor {
    public Oklch {
      if (alpha < 0 || alpha > 1) {
        throw new IllegalArgumentException("Alpha must be within [0, 1]: " + alpha);
      }
    }

    @Override
    public String toCss() {
      var base =
          "oklch("
              + CssLength.formatNumber(lightness)
              + "% "
              + CssLength.formatNumber(chroma)
              + " "
              + CssLength.formatNumber(hue);
      return alpha < 1 ? base + " / " + CssLength.formatNumber(alpha) + ")" : base + ")";
    }
  }

  /// `#rrggbb`.
  record Srgb(int rgb) implements CssColor {
    public Srgb {
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

  /// Interpolation color spaces for [Mix].
  enum Space implements CssKeyword {
    OKLCH,
    OKLAB
  }

  /// `color-mix(in <space>, first <weight>, second)`; `weight` is the share of `first`.
  record Mix(Space space, CssColor first, CssLength weight, CssColor second) implements CssColor {
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
          + CssLength.formatNumber(lightness)
          + "% "
          + CssLength.formatNumber(chroma)
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

  /// `oklch(L% C H)` with lightness as a percentage, such as `oklch(54% 0.21 262)`.
  static CssColor oklch(double lightnessPercent, double chroma, double hue) {
    return new Oklch(lightnessPercent, chroma, hue, 1);
  }

  /// `oklch(L% C H / A)`, or `oklch(L% C H)` at full opacity. Throws IllegalArgumentException if
  /// `alpha` is outside `[0, 1]`.
  static CssColor oklch(double lightnessPercent, double chroma, double hue, double alpha) {
    return new Oklch(lightnessPercent, chroma, hue, alpha);
  }

  /// `#rrggbb` from a 24-bit value, such as `0x1d4ed8`. Throws IllegalArgumentException outside
  /// `0x000000` to `0xffffff`.
  static CssColor srgb(int rgb) {
    return new Srgb(rgb);
  }

  /// `#ffffff`.
  static CssColor white() {
    return new Srgb(0xffffff);
  }

  /// `transparent`.
  static CssColor transparent() {
    return Named.TRANSPARENT;
  }

  /// `currentColor`, the element's `color` value.
  static CssColor currentColor() {
    return Named.CURRENT_COLOR;
  }

  /// `oklch(from base L% C h)`: `base`'s hue at a fixed lightness (a percentage) and chroma.
  static CssColor sameHue(CssColor base, double lightnessPercent, double chroma) {
    return new Relative(base, lightnessPercent, chroma);
  }

  /// `light-dark(light, dark)`, following the page's color scheme.
  static CssColor lightDark(CssColor light, CssColor dark) {
    return new LightDark(light, dark);
  }

  /// This color at `percent` of full strength over transparency, mixed in OKLCH.
  default CssColor mixTransparent(int percent) {
    return mix(Space.OKLCH, percent, transparent());
  }

  /// This color at `percent`% weight against `other`, mixed in `space`.
  default CssColor mix(Space space, int percent, CssColor other) {
    return mix(space, CssLength.pct(percent), other);
  }

  /// This color weighted by `weight` (a percentage or a calculation of one) against `other`,
  /// mixed in `space`.
  default CssColor mix(Space space, CssLength weight, CssColor other) {
    return new Mix(space, this, weight, other);
  }

  /// A readable foreground for this color: `contrast-color(this)`, black or white.
  default CssColor contrast() {
    return new ContrastOn(this);
  }
}
