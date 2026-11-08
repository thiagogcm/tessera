package dev.nthings.tessera.css;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/// A validated CSS identifier: a container, anchor, counter, or view-transition name, or a custom
/// property name.
public record CssIdent(String value) implements CssValue {

  private static final Pattern IDENT =
      Pattern.compile("--[A-Za-z0-9_-]+|-?[A-Za-z_][A-Za-z0-9_-]*");

  /// Rejected as names: the CSS-wide keywords, `default`, and `none`, which name-taking
  /// properties also accept as values.
  private static final Set<String> RESERVED =
      Set.of("initial", "inherit", "unset", "revert", "revert-layer", "default", "none");

  public CssIdent {
    if (!IDENT.matcher(value).matches()) {
      throw new IllegalArgumentException("Not a CSS identifier: '" + value + "'");
    }
  }

  /// Non-dashed name. Reserved keywords are rejected in any case.
  public static CssIdent of(String name) {
    var ident = new CssIdent(name);
    if (ident.isCustom()) {
      throw new IllegalArgumentException(
          "Use CssIdent.custom(...) for dashed identifiers: '" + name + "'");
    }
    if (RESERVED.contains(name.toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException("'" + name + "' is a CSS keyword, not a name");
    }
    return ident;
  }

  /// Dashed identifier `--<name>`.
  public static CssIdent custom(String name) {
    return new CssIdent("--" + name);
  }

  public boolean isCustom() {
    return value.startsWith("--");
  }

  /// Throws unless this is a dashed (`--name`) identifier.
  public CssIdent requireCustom() {
    if (!isCustom()) {
      throw new IllegalArgumentException("Not a custom property name: " + value);
    }
    return this;
  }

  /// `var(--name)`; callers must have checked [#requireCustom()].
  String varReference() {
    return "var(" + value + ")";
  }

  @Override
  public String toCss() {
    return value;
  }
}
