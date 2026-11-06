package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.Fnv1a64;

/// CSS custom property registered with [Tokens]. Each category exposes only the operations that
/// match its CSS type, and its value is a typed [CssValue] of that category.
public sealed interface DesignToken
    permits DesignToken.Color,
        DesignToken.Length,
        DesignToken.Duration,
        DesignToken.FontFamily,
        DesignToken.Number {

  String name();

  /// The typed value the custom property is defined as.
  CssValue value();

  /// The value as CSS, the right-hand side of the custom property declaration.
  default String cssDefinition() {
    return value().toCss();
  }

  default String customPropertyName() {
    return "--" + name();
  }

  default String varReference() {
    return "var(--" + name() + ")";
  }

  /// Color token. Adaptive colors use [Color#lightDark].
  record Color(String name, CssColor value) implements DesignToken, CssColor {
    public Color {
      name = Fnv1a64.sanitizeIdentifier(name);
    }

    /// The color as `var(--name)`, usable wherever a [CssColor] is accepted.
    @Override
    public String toCss() {
      return varReference();
    }

    /// The same token with a different value, for theme overrides that keep its category.
    public Color withValue(CssColor replacement) {
      return new Color(name, replacement);
    }

    public static Color lightDark(String name, CssColor light, CssColor dark) {
      return new Color(name, CssColor.lightDark(light, dark));
    }

    /// The same token with an adaptive value, for theme overrides.
    public Color withLightDark(CssColor light, CssColor dark) {
      return withValue(CssColor.lightDark(light, dark));
    }

    /// Role whose value is another color token, so a theme can replace the role or the source.
    public static Color alias(String name, Color source) {
      return new Color(name, source);
    }
  }

  /// Length token. Spacing tokens multiply a length by `--density`; other lengths do not.
  record Length(String name, CssLength value) implements DesignToken, CssLength {
    public Length {
      name = Fnv1a64.sanitizeIdentifier(name);
    }

    /// The length as `var(--name)`, usable wherever a [CssLength] is accepted.
    @Override
    public String toCss() {
      return varReference();
    }

    /// The same token with a different value, for theme overrides that keep its category.
    public Length withValue(CssLength replacement) {
      return new Length(name, replacement);
    }

    public static Length alias(String name, Length source) {
      return new Length(name, source);
    }
  }

  /// Time token used by [Motion].
  record Duration(String name, CssDuration value) implements DesignToken, CssDuration {
    public Duration {
      name = Fnv1a64.sanitizeIdentifier(name);
    }

    @Override
    public String toCss() {
      return varReference();
    }

    /// The same token with a different value, for theme overrides that keep its category.
    public Duration withValue(CssDuration replacement) {
      return new Duration(name, replacement);
    }
  }

  record FontFamily(String name, FontStack value) implements DesignToken, FontStack.Entry {
    public FontFamily {
      name = Fnv1a64.sanitizeIdentifier(name);
    }

    /// The font family as `var(--name)`, usable wherever a font stack entry is.
    @Override
    public String toCss() {
      return varReference();
    }

    /// The same token with a different value, for theme overrides that keep its category.
    public FontFamily withValue(FontStack replacement) {
      return new FontFamily(name, replacement);
    }

    public static FontFamily alias(String name, FontFamily source) {
      return new FontFamily(name, FontStack.of(source));
    }
  }

  /// Unitless number, such as line height or density.
  record Number(String name, CssNumber value) implements DesignToken, CssNumber {
    public Number {
      name = Fnv1a64.sanitizeIdentifier(name);
    }

    public static Number of(String name, double value) {
      return new Number(name, CssNumber.of(value));
    }

    @Override
    public String toCss() {
      return varReference();
    }

    /// The same token with a different value, for theme overrides that keep its category.
    public Number withValue(double replacement) {
      return new Number(name, CssNumber.of(replacement));
    }

    public static Number alias(String name, Number source) {
      return new Number(name, source);
    }
  }
}
