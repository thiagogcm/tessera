package dev.nthings.tessera.css;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// The values of the design tokens, emitted as custom properties on `:root` in `@layer theme`.
/// Immutable: start from [#defaults()] and override tokens with [#with(DesignToken, CssValue)],
/// which returns a new theme, then pass it to `Compiler.compile` or `SiteDefinition.Builder#theme`.
///
/// ```java
/// var theme = Theme.defaults()
///     .with(Tokens.ACCENT_PRIMARY, CssColor.oklch(55, 0.18, 150))
///     .with(Tokens.DENSITY, CssNumber.of(0.9))
///     .with(Tokens.RADIUS_XL, CssLength.rem(1));
/// ```
public final class Theme {

  private record Entry(DesignToken<?> token, CssValue value) {}

  private static final Theme DEFAULTS = defaultTheme();

  private final Map<String, Entry> entries;

  private Theme(Map<String, Entry> entries) {
    this.entries = entries;
  }

  /// Every token in [Tokens] at its default value.
  public static Theme defaults() {
    return DEFAULTS;
  }

  /// A theme where `token` has `value`, keeping its position; a token this theme does not define
  /// yet, such as one of your own, is added at the end. The value must be of the token's category.
  /// A token may take another token of its category, such as `with(Tokens.CONTROL_FG,
  /// Tokens.ACCENT_TEXT)`, to follow it. Throws IllegalArgumentException if the theme already has a
  /// token of a different category with the same name.
  public <V extends CssValue> Theme with(DesignToken<V> token, V value) {
    var existing = entries.get(token.name());
    if (existing != null && existing.token().getClass() != token.getClass()) {
      throw new IllegalArgumentException(
          "Token '"
              + CssIdent.custom(token.name()).toCss()
              + "' is already a "
              + existing.token().getClass().getSimpleName()
              + " token");
    }
    var next = new LinkedHashMap<>(entries);
    next.put(token.name(), new Entry(token, value));
    return new Theme(next);
  }

  /// The `--name:value` declarations, in theme order.
  List<String> declarations() {
    return entries.values().stream()
        .map(entry -> CssIdent.custom(entry.token().name()).toCss() + ":" + entry.value().toCss())
        .toList();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof Theme theme && theme.declarations().equals(declarations());
  }

  @Override
  public int hashCode() {
    return declarations().hashCode();
  }

  @Override
  public String toString() {
    return String.join(";", declarations());
  }

  private static Theme defaultTheme() {
    var entries = new LinkedHashMap<String, Entry>();
    for (var token : Tokens.ALL) {
      entries.put(token.name(), new Entry(token, token.defaultValue()));
    }
    return new Theme(entries);
  }
}
