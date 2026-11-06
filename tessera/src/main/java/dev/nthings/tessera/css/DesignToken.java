package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.Fnv1a64;

/// CSS custom property registered with [Tokens]. Each category exposes only the operations that
/// match its CSS type.
public sealed interface DesignToken
    permits DesignToken.Color,
        DesignToken.Length,
        DesignToken.Duration,
        DesignToken.FontFamily,
        DesignToken.Number {

  String name();

  String cssDefinition();

  default String customPropertyName() {
    return "--" + name();
  }

  default String varReference() {
    return "var(--" + name() + ")";
  }

  /// Color token. Adaptive colors use [Color#lightDark].
  record Color(String name, String cssDefinition) implements DesignToken {
    public Color {
      name = Fnv1a64.sanitizeIdentifier(name);
    }

    /// The same token with a different CSS value, for theme overrides that keep its category.
    public Color withValue(String css) {
      return new Color(name, css);
    }

    public static Color lightDark(String name, String lightValue, String darkValue) {
      return new Color(name, lightDarkCss(lightValue, darkValue));
    }

    /// The same token with an adaptive value, for theme overrides.
    public Color withLightDark(String lightValue, String darkValue) {
      return withValue(lightDarkCss(lightValue, darkValue));
    }

    private static String lightDarkCss(String lightValue, String darkValue) {
      return "light-dark(" + lightValue + ", " + darkValue + ")";
    }

    /// Role whose value is another color token, so a theme can replace the role or the source.
    public static Color alias(String name, Color source) {
      return new Color(name, source.varReference());
    }

    /// `color-mix(in oklch, var(--name) <percent>%, transparent)`.
    public String mixTransparent(int percent) {
      return "color-mix(in oklch, " + varReference() + " " + percent + "%, transparent)";
    }
  }

  /// Length token. [Length#space] multiplies a length by `--density`; other lengths do not.
  record Length(String name, String cssDefinition) implements DesignToken, CssLength {
    public Length {
      name = Fnv1a64.sanitizeIdentifier(name);
    }

    /// The same token with a different CSS value, for theme overrides that keep its category.
    public Length withValue(String css) {
      return new Length(name, css);
    }

    public static Length space(String name, String size) {
      return new Length(name, "calc(" + size + " * var(--density))");
    }

    public static Length alias(String name, Length source) {
      return new Length(name, source.varReference());
    }

    /// The length as `var(--name)`, usable wherever a [CssLength] is accepted.
    @Override
    public String toCss() {
      return varReference();
    }
  }

  /// Time token used by [Motion].
  record Duration(String name, String cssDefinition) implements DesignToken {
    public Duration {
      name = Fnv1a64.sanitizeIdentifier(name);
    }

    /// The same token with a different CSS value, for theme overrides that keep its category.
    public Duration withValue(String css) {
      return new Duration(name, css);
    }
  }

  record FontFamily(String name, String cssDefinition) implements DesignToken {
    public FontFamily {
      name = Fnv1a64.sanitizeIdentifier(name);
    }

    /// The same token with a different CSS value, for theme overrides that keep its category.
    public FontFamily withValue(String css) {
      return new FontFamily(name, css);
    }

    public static FontFamily alias(String name, FontFamily source) {
      return new FontFamily(name, source.varReference());
    }
  }

  /// Unitless number, such as line height or density.
  record Number(String name, String cssDefinition) implements DesignToken {
    public Number {
      name = Fnv1a64.sanitizeIdentifier(name);
    }

    /// The same token with a different CSS value, for theme overrides that keep its category.
    public Number withValue(String css) {
      return new Number(name, css);
    }

    public static Number alias(String name, Number source) {
      return new Number(name, source.varReference());
    }
  }
}
