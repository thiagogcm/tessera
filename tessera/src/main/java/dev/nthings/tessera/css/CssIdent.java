package dev.nthings.tessera.css;

import java.util.regex.Pattern;

/// A validated CSS identifier: a container, anchor, counter, or view-transition name, or a custom
/// property name.
public record CssIdent(String value) implements CssValue {

  private static final Pattern IDENT =
      Pattern.compile("--[A-Za-z0-9_-]+|-?[A-Za-z_][A-Za-z0-9_-]*");

  public CssIdent {
    if (!IDENT.matcher(value).matches()) {
      throw new IllegalArgumentException("Not a CSS identifier: '" + value + "'");
    }
  }

  /// An ordinary identifier such as `card` or `code-line`.
  public static CssIdent of(String name) {
    var ident = new CssIdent(name);
    if (ident.isCustom()) {
      throw new IllegalArgumentException(
          "Use CssIdent.custom(...) for dashed identifiers: '" + name + "'");
    }
    return ident;
  }

  /// A dashed identifier, `--<name>`, naming a custom property or anchor.
  public static CssIdent custom(String name) {
    return new CssIdent("--" + name);
  }

  public boolean isCustom() {
    return value.startsWith("--");
  }

  /// This identifier, which must name a custom property (`--name`).
  public CssIdent requireCustom() {
    if (!isCustom()) {
      throw new IllegalArgumentException("Not a custom property name: " + value);
    }
    return this;
  }

  /// `var(--name)`, a reference to the custom property this identifier names.
  public String varReference() {
    return "var(" + requireCustom().value + ")";
  }

  @Override
  public String toCss() {
    return value;
  }
}
