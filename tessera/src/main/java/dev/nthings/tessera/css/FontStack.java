package dev.nthings.tessera.css;

import java.util.List;
import java.util.stream.Collectors;
import org.owasp.encoder.Encode;

/// A `font-family` list of named families, generic fallbacks, and font family tokens. Set it with
/// [Styled#fontFamily(FontStack)], or give a [DesignToken.FontFamily] a stack with `withValue`.
public record FontStack(List<Entry> entries) implements CssValue {

  /// One item of a [FontStack]: a named family from [FontStack#named(String)], a [Generic] family,
  /// or a font family token.
  public sealed interface Entry extends CssValue permits Named, Generic, DesignToken.FontFamily {}

  /// A family looked up by name; names that are not plain identifiers are quoted.
  record Named(String family) implements Entry {
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

  /// A stack of `entries` in order, such as `Georgia,serif`. Throws IllegalArgumentException with
  /// no entries.
  public static FontStack of(Entry... entries) {
    return new FontStack(List.of(entries));
  }

  /// A family looked up by name, quoted in CSS when it is not a plain identifier, such as
  /// `"Fira Code"`.
  public static Entry named(String family) {
    return new Named(family);
  }

  @Override
  public String toCss() {
    return entries.stream().map(Entry::toCss).collect(Collectors.joining(","));
  }
}
