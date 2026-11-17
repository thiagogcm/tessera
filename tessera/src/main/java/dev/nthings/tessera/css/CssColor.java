package dev.nthings.tessera.css;

import com.google.errorprone.annotations.CheckReturnValue;

/// A CSS color: OKLCH and sRGB literals, keywords, derived colors (`color-mix`, relative
/// `oklch(from ...)`, `light-dark`, `contrast-color`), and design tokens. Build values with the
/// static factories and the derivation methods.
@CheckReturnValue
public sealed interface CssColor extends CssValue
    permits CssColors.Oklch,
        CssColors.Srgb,
        CssColors.Named,
        CssColors.Mix,
        CssColors.Relative,
        CssColors.LightDark,
        CssColors.ContrastOn,
        DesignToken.Color {

  /// Interpolation color spaces for [#mix(Space, int, CssColor)].
  enum Space implements CssKeyword {
    OKLCH,
    OKLAB
  }

  /// `oklch(L% C H)` with lightness as a percentage, such as `oklch(54% 0.21 262)`.
  static CssColor oklch(double lightnessPercent, double chroma, double hue) {
    return new CssColors.Oklch(lightnessPercent, chroma, hue, 1);
  }

  /// `oklch(L% C H / A)`, or `oklch(L% C H)` at full opacity. Throws IllegalArgumentException if
  /// `alpha` is outside `[0, 1]`.
  static CssColor oklch(double lightnessPercent, double chroma, double hue, double alpha) {
    return new CssColors.Oklch(lightnessPercent, chroma, hue, alpha);
  }

  /// `#rrggbb` from a 24-bit value, such as `0x1d4ed8`. Throws IllegalArgumentException outside
  /// `0x000000` to `0xffffff`.
  static CssColor srgb(int rgb) {
    return new CssColors.Srgb(rgb);
  }

  /// `#ffffff`.
  static CssColor white() {
    return new CssColors.Srgb(0xffffff);
  }

  /// `transparent`.
  static CssColor transparent() {
    return CssColors.Named.TRANSPARENT;
  }

  /// `currentColor`, the element's `color` value.
  static CssColor currentColor() {
    return CssColors.Named.CURRENT_COLOR;
  }

  /// `oklch(from base L% C h)`: `base`'s hue at a fixed lightness (a percentage) and chroma.
  static CssColor sameHue(CssColor base, double lightnessPercent, double chroma) {
    return new CssColors.Relative(base, lightnessPercent, chroma);
  }

  /// `light-dark(light, dark)`, following the page's color scheme.
  static CssColor lightDark(CssColor light, CssColor dark) {
    return new CssColors.LightDark(light, dark);
  }

  /// This color at `percent` of full strength over transparency, mixed in OKLCH.
  default CssColor mixTransparent(int percent) {
    return mix(Space.OKLCH, percent, transparent());
  }

  /// This color at `percent`% weight against `other`, mixed in `space`.
  default CssColor mix(Space space, int percent, CssColor other) {
    return mix(space, CssLength.pct(percent), other);
  }

  /// This color weighted by `weight` (a percentage or a calculation of one) against `other`, mixed
  /// in `space`.
  default CssColor mix(Space space, CssLength weight, CssColor other) {
    return new CssColors.Mix(space, this, weight, other);
  }

  /// A readable foreground for this color: `contrast-color(this)`, black or white.
  default CssColor contrast() {
    return new CssColors.ContrastOn(this);
  }
}
