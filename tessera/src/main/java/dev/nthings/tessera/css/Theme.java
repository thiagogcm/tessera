package dev.nthings.tessera.css;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.internal.css.CssAccess;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// The values of the design tokens, emitted as custom properties on `:root` in `@layer theme`.
/// Immutable: start from [#defaults()] and override tokens with [#with(DesignToken, CssValue)],
/// which returns a new theme, then pass it to `Compiler.withTheme` or `MountOptions.withTheme`.
/// Both reject a theme whose text colors fall below WCAG AA contrast on their surfaces; see
/// [#contrastDiagnostics()].
///
/// ```java
/// var theme = Theme.defaults()
///     .with(Tokens.ACCENT_PRIMARY,
///         CssColor.lightDark(CssColor.oklch(50, 0.16, 150), CssColor.oklch(72, 0.16, 150)))
///     .with(Tokens.DENSITY, CssNumber.of(0.9))
///     .with(Tokens.RADIUS_XL, CssLength.rem(1));
/// ```
@CheckReturnValue
public final class Theme {

  private record Entry(DesignToken<?> token, CssValue value) {}

  static {
    CssAccess.register(new CssCollection());
  }

  private static final Theme DEFAULTS = defaultTheme();

  private final Map<String, Entry> entries;

  /// [#contrastDiagnostics()], computed on first use. The theme is immutable, so a race computes
  /// the same immutable list twice at worst.
  private volatile @Nullable List<Diagnostic> contrast;

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
          "Token '--"
              + token.name()
              + "' is already a "
              + existing.token().getClass().getSimpleName()
              + " token");
    }
    var next = new LinkedHashMap<>(entries);
    next.put(token.name(), new Entry(token, value));
    return new Theme(next);
  }

  /// The WCAG AA contrast failures of this theme as `theme-contrast` diagnostics, empty when the
  /// theme passes. Checks every text and surface pair that built-in components draw from color
  /// tokens, such as [Tokens#TEXT_MUTED] on [Tokens#SURFACE_SUBTLE] or a [Tone]'s label on its
  /// solid fill, against 4.5:1 in both the light and the dark color scheme. Token aliases,
  /// `light-dark()`, `sameHue`, `color-mix()` and `contrast-color()` resolve statically; a pair is
  /// skipped when a color depends on the page, such as `currentColor`, a token this theme does not
  /// define, or a cycle of token aliases. Each backend rejects a theme with diagnostics where it
  /// accepts the theme: `Compiler.withTheme` and `MountOptions.withTheme`. The theme computes them
  /// once.
  public List<Diagnostic> contrastDiagnostics() {
    var computed = contrast;
    if (computed == null) {
      computed = ThemeContrast.diagnostics(this);
      contrast = computed;
    }
    return computed;
  }

  /// The value this theme gives the token named `name`, or null if it does not define one.
  @Nullable CssValue value(String name) {
    var entry = entries.get(name);
    return entry == null ? null : entry.value();
  }

  /// The `--name:value` declarations, in theme order. Token names were validated when the tokens
  /// were constructed.
  List<String> declarations() {
    return entries.values().stream()
        .map(entry -> "--" + entry.token().name() + ":" + entry.value().toCss())
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
