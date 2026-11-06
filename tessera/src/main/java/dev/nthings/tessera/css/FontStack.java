package dev.nthings.tessera.css;

import java.util.List;
import java.util.stream.Collectors;
import org.owasp.encoder.Encode;

/// A `font-family` list of named families and generic fallbacks, or a design token.
public record FontStack(List<Entry> entries) implements CssValue {

  /// One family in the list.
  public sealed interface Entry extends CssValue permits Named, Generic, DesignToken.FontFamily {}

  /// A family looked up by name; names that are not plain identifiers are quoted.
  public record Named(String family) implements Entry {
    @Override
    public String toCss() {
      return family.matches("[A-Za-z_-][A-Za-z0-9_-]*")
          ? family
          : "\"" + Encode.forCssString(family) + "\"";
    }
  }

  /// Generic families resolved by the platform.
  public enum Generic implements Entry, CssKeyword {
    SERIF,
    SANS_SERIF,
    MONOSPACE,
    SYSTEM_UI,
    UI_MONOSPACE
  }

  public FontStack {
    if (entries.isEmpty()) {
      throw new IllegalArgumentException("A font stack needs at least one family");
    }
    entries = List.copyOf(entries);
  }

  public static FontStack of(Entry... entries) {
    return new FontStack(List.of(entries));
  }

  public static Entry named(String family) {
    return new Named(family);
  }

  @Override
  public String toCss() {
    return entries.stream().map(Entry::toCss).collect(Collectors.joining(","));
  }
}
