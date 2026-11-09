package dev.nthings.tessera.css;

/// A theme-wide CSS custom property with a default value of type `V`. A token is itself a value of
/// its category, serializing as `var(--name)`, so `bg(Tokens.SURFACE_CARD)` works wherever a
/// color does. [Theme#with(DesignToken, CssValue)] overrides it with a value of the same category,
/// so a length cannot replace a color. [Tokens] holds the built-in tokens; create your own with a
/// category's constructor, such as `new DesignToken.Color("brand", CssColor.oklch(60, 0.2, 300))`,
/// and add it to a theme with `Theme.with`.
///
/// Pass the name without its leading `--`. Every constructor throws IllegalArgumentException for
/// an empty name, a name starting with `-`, or one with characters outside `[A-Za-z0-9_-]`.
///
/// @param <V> the value category, such as [CssColor]
public sealed interface DesignToken<V extends CssValue> extends CssValue
    permits DesignToken.Color,
        DesignToken.Length,
        DesignToken.Duration,
        DesignToken.FontFamily,
        DesignToken.Number {

  /// The custom property name without its leading `--`, such as `space-4`.
  String name();

  /// The value [Theme#defaults()] gives the token.
  V defaultValue();

  /// `var(--name)`.
  @Override
  default String toCss() {
    return CssIdent.custom(name()).varReference();
  }

  /// A color token. Use `CssColor.lightDark(light, dark)` for a value that follows the color
  /// scheme, or another color token to alias it.
  record Color(String name, CssColor defaultValue) implements DesignToken<CssColor>, CssColor {
    public Color {
      CssIdent.custom(name);
    }
  }

  /// A length token. Spacing tokens in [Tokens] multiply by `--density`; other lengths do not.
  record Length(String name, CssLength defaultValue) implements DesignToken<CssLength>, CssLength {
    public Length {
      CssIdent.custom(name);
    }
  }

  /// A time token, used by [Transition]s.
  record Duration(String name, CssDuration defaultValue)
      implements DesignToken<CssDuration>, CssDuration {
    public Duration {
      CssIdent.custom(name);
    }
  }

  /// A font family token, used with [Styled#fontFamily(DesignToken.FontFamily)] or as a
  /// [FontStack] entry.
  record FontFamily(String name, FontStack defaultValue)
      implements DesignToken<FontStack>, FontStack.Entry {
    public FontFamily {
      CssIdent.custom(name);
    }
  }

  /// A unitless number token, such as a line height or the density multiplier.
  record Number(String name, CssNumber defaultValue) implements DesignToken<CssNumber>, CssNumber {
    public Number {
      CssIdent.custom(name);
    }
  }
}
