package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.Fnv1a64;

/// A CSS custom property registered with [Tokens]. Each category takes only values of its CSS
/// type, so a length cannot replace a color. Pass the name without its leading `--`: characters
/// outside `[A-Za-z0-9_-]` become `-`, and an empty name throws IllegalArgumentException.
public sealed interface DesignToken
    permits DesignToken.Color,
        DesignToken.Length,
        DesignToken.Duration,
        DesignToken.FontFamily,
        DesignToken.Number {

  String name();

  CssValue value();

  /// Compiler-facing: the right-hand side of the custom property declaration.
  default String cssDefinition() {
    return value().toCss();
  }

  /// Compiler-facing: the declared custom property name, `--name`.
  default String customPropertyName() {
    return "--" + name();
  }

  /// Compiler-facing: the `var(--name)` reference that components emit.
  default String varReference() {
    return "var(--" + name() + ")";
  }

  /// A color token. Use [Color#lightDark] for a value that follows the color scheme.
  record Color(String name, CssColor value) implements DesignToken, CssColor {
    public Color {
      name = Fnv1a64.sanitizeIdentifier(name);
    }

    /// `var(--name)`.
    @Override
    public String toCss() {
      return varReference();
    }

    /// Same token with a new value, for theme overrides.
    public Color withValue(CssColor replacement) {
      return new Color(name, replacement);
    }

    /// A color token whose value is `light-dark(light, dark)`.
    public static Color lightDark(String name, CssColor light, CssColor dark) {
      return new Color(name, CssColor.lightDark(light, dark));
    }

    /// Same token with light and dark values, for theme overrides.
    public Color withLightDark(CssColor light, CssColor dark) {
      return withValue(CssColor.lightDark(light, dark));
    }

    /// A token whose value is `var(--source)`, so overriding `source` also changes this token.
    public static Color alias(String name, Color source) {
      return new Color(name, source);
    }
  }

  /// A length token. Spacing tokens in [Tokens] multiply by `--density`; other lengths do not.
  record Length(String name, CssLength value) implements DesignToken, CssLength {
    public Length {
      name = Fnv1a64.sanitizeIdentifier(name);
    }

    /// `var(--name)`.
    @Override
    public String toCss() {
      return varReference();
    }

    /// Same token with a new value, for theme overrides.
    public Length withValue(CssLength replacement) {
      return new Length(name, replacement);
    }

    /// A token whose value is `var(--source)`, so overriding `source` also changes this token.
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

    /// Same token with a new value, for theme overrides.
    public Duration withValue(CssDuration replacement) {
      return new Duration(name, replacement);
    }
  }

  /// A font family token, used with [TextStyled#fontFamily(DesignToken.FontFamily)] or as a
  /// [FontStack] entry.
  record FontFamily(String name, FontStack value) implements DesignToken, FontStack.Entry {
    public FontFamily {
      name = Fnv1a64.sanitizeIdentifier(name);
    }

    /// `var(--name)`.
    @Override
    public String toCss() {
      return varReference();
    }

    /// Same token with a new value, for theme overrides.
    public FontFamily withValue(FontStack replacement) {
      return new FontFamily(name, replacement);
    }

    /// A token whose value is `var(--source)`, so overriding `source` also changes this token.
    public static FontFamily alias(String name, FontFamily source) {
      return new FontFamily(name, FontStack.of(source));
    }
  }

  /// Unitless number, such as line height or density.
  record Number(String name, CssNumber value) implements DesignToken, CssNumber {
    public Number {
      name = Fnv1a64.sanitizeIdentifier(name);
    }

    /// A number token holding `value`, referenced as `var(--name)`.
    public static Number of(String name, double value) {
      return new Number(name, CssNumber.of(value));
    }

    @Override
    public String toCss() {
      return varReference();
    }

    /// Same token with a new value, for theme overrides.
    public Number withValue(double replacement) {
      return new Number(name, CssNumber.of(replacement));
    }

    /// A token whose value is `var(--source)`, so overriding `source` also changes this token.
    public static Number alias(String name, Number source) {
      return new Number(name, source);
    }
  }
}
