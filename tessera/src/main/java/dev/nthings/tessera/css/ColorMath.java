package dev.nthings.tessera.css;

import java.util.Locale;

/// Deterministic color arithmetic behind [Theme#contrastDiagnostics()]: OKLCH and sRGB to OKLab,
/// `color-mix()` interpolation, source-over compositing and WCAG 2.x relative luminance. It uses
/// [StrictMath] throughout, so a theme reports the same ratios on every platform.
///
/// Colors outside the sRGB gamut are clipped per channel, as browsers paint them on sRGB displays.
final class ColorMath {

  /// Chroma at or below which a color is achromatic: its hue is powerless, and interpolation in
  /// OKLCH takes the other color's hue instead.
  private static final double ACHROMATIC = 1e-4;

  /// `transparent`: black at zero opacity.
  static final Lab TRANSPARENT = new Lab(0, 0, 0, 0);

  private ColorMath() {}

  /// A color in OKLab with straight (not premultiplied) alpha. `l` is lightness in `[0, 1]`.
  record Lab(double l, double a, double b, double alpha) {

    double chroma() {
      return StrictMath.hypot(a, b);
    }

    /// The OKLCH hue in degrees within `[0, 360)`, or 0 for an achromatic color.
    double hue() {
      if (chroma() <= ACHROMATIC) {
        return 0;
      }
      var degrees = StrictMath.toDegrees(StrictMath.atan2(b, a));
      return degrees < 0 ? degrees + 360 : degrees;
    }

    boolean opaque() {
      return alpha >= 1;
    }
  }

  /// `oklch(L% C H / alpha)`. Lightness clamps to `[0, 100]` and chroma to non-negative values, as
  /// CSS does when parsing.
  static Lab oklch(double lightnessPercent, double chroma, double hue, double alpha) {
    var lightness = StrictMath.min(StrictMath.max(lightnessPercent, 0), 100) / 100;
    var c = StrictMath.max(chroma, 0);
    var radians = StrictMath.toRadians(hue);
    return new Lab(lightness, c * StrictMath.cos(radians), c * StrictMath.sin(radians), alpha);
  }

  /// A 24-bit sRGB color at `alpha`.
  static Lab srgb(int rgb, double alpha) {
    return fromLinear(
        decode(((rgb >>> 16) & 0xff) / 255.0),
        decode(((rgb >>> 8) & 0xff) / 255.0),
        decode((rgb & 0xff) / 255.0),
        alpha);
  }

  /// `color-mix(in space, first share, second)`, where `share` in `[0, 1]` is the weight of
  /// `first`. Components interpolate premultiplied by alpha; in OKLCH the hue takes the shorter arc
  /// and an achromatic color adopts the other color's hue.
  static Lab mix(CssColor.Space space, Lab first, double share, Lab second) {
    var rest = 1 - share;
    var w1 = first.alpha() * share;
    var w2 = second.alpha() * rest;
    var alpha = w1 + w2;
    if (alpha <= 0) {
      return TRANSPARENT;
    }
    var lightness = (first.l() * w1 + second.l() * w2) / alpha;
    return switch (space) {
      case OKLAB ->
          new Lab(
              lightness,
              (first.a() * w1 + second.a() * w2) / alpha,
              (first.b() * w1 + second.b() * w2) / alpha,
              alpha);
      case OKLCH -> {
        var chroma = (first.chroma() * w1 + second.chroma() * w2) / alpha;
        var hue = interpolateHue(first, share, second);
        var radians = StrictMath.toRadians(hue);
        yield new Lab(
            lightness, chroma * StrictMath.cos(radians), chroma * StrictMath.sin(radians), alpha);
      }
    };
  }

  private static double interpolateHue(Lab first, double share, Lab second) {
    var firstMissing = first.chroma() <= ACHROMATIC;
    var secondMissing = second.chroma() <= ACHROMATIC;
    if (firstMissing || secondMissing) {
      return firstMissing ? second.hue() : first.hue();
    }
    var h1 = first.hue();
    var h2 = second.hue();
    if (h2 - h1 > 180) {
      h1 += 360;
    } else if (h2 - h1 < -180) {
      h2 += 360;
    }
    return h1 * share + h2 * (1 - share);
  }

  /// `top` painted over the opaque `bottom` with source-over blending in gamma-encoded sRGB, as
  /// browsers composite.
  static Lab over(Lab top, Lab bottom) {
    if (top.opaque()) {
      return top;
    }
    var upper = encoded(top);
    var lower = encoded(bottom);
    var alpha = top.alpha();
    return fromLinear(
        decode(upper[0] * alpha + lower[0] * (1 - alpha)),
        decode(upper[1] * alpha + lower[1] * (1 - alpha)),
        decode(upper[2] * alpha + lower[2] * (1 - alpha)),
        1);
  }

  /// WCAG 2.x relative luminance of an opaque color, after clipping to the sRGB gamut.
  static double luminance(Lab color) {
    var rgb = linear(color);
    return 0.2126 * clip(rgb[0]) + 0.7152 * clip(rgb[1]) + 0.0722 * clip(rgb[2]);
  }

  /// The WCAG 2.x contrast ratio of two opaque colors, from 1 to 21.
  static double contrast(Lab first, Lab second) {
    var y1 = luminance(first);
    var y2 = luminance(second);
    return (StrictMath.max(y1, y2) + 0.05) / (StrictMath.min(y1, y2) + 0.05);
  }

  /// `#rrggbb` of an opaque color, clipped to the sRGB gamut and rounded to 8-bit channels.
  static String hex(Lab color) {
    var rgb = encoded(color);
    return String.format(
        Locale.ROOT,
        "#%02x%02x%02x",
        StrictMath.round(rgb[0] * 255),
        StrictMath.round(rgb[1] * 255),
        StrictMath.round(rgb[2] * 255));
  }

  private static Lab fromLinear(double r, double g, double b, double alpha) {
    var l = StrictMath.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
    var m = StrictMath.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
    var s = StrictMath.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
    return new Lab(
        0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
        1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
        0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
        alpha);
  }

  /// Linear sRGB channels, unclipped.
  private static double[] linear(Lab color) {
    var l = cube(color.l() + 0.3963377774 * color.a() + 0.2158037573 * color.b());
    var m = cube(color.l() - 0.1055613458 * color.a() - 0.0638541728 * color.b());
    var s = cube(color.l() - 0.0894841775 * color.a() - 1.2914855480 * color.b());
    return new double[] {
      4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
      -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
      -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
    };
  }

  /// Gamma-encoded sRGB channels in `[0, 1]`.
  private static double[] encoded(Lab color) {
    var rgb = linear(color);
    return new double[] {encode(rgb[0]), encode(rgb[1]), encode(rgb[2])};
  }

  private static double cube(double value) {
    return value * value * value;
  }

  private static double clip(double channel) {
    return StrictMath.min(StrictMath.max(channel, 0), 1);
  }

  private static double encode(double linear) {
    var channel = clip(linear);
    return channel <= 0.0031308
        ? 12.92 * channel
        : 1.055 * StrictMath.pow(channel, 1 / 2.4) - 0.055;
  }

  private static double decode(double encoded) {
    return encoded <= 0.04045 ? encoded / 12.92 : StrictMath.pow((encoded + 0.055) / 1.055, 2.4);
  }
}
