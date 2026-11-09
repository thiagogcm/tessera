package dev.nthings.tessera.css;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/// A validated CSS identifier, the name inside the public handles ([Container], [Keyframes],
/// [Anchor], [Counter], [CustomProperty]). [#of] makes a plain name and [#custom] a dashed one.
record CssIdent(String value) implements CssValue {

  private static final Pattern IDENT =
      Pattern.compile("--[A-Za-z0-9_-]+|-?[A-Za-z_][A-Za-z0-9_-]*");

  /// Rejected as names: the CSS-wide keywords, `default`, and `none`, which name-taking
  /// properties also accept as values.
  private static final Set<String> RESERVED =
      Set.of("initial", "inherit", "unset", "revert", "revert-layer", "default", "none");

  CssIdent {
    if (!IDENT.matcher(value).matches()) {
      throw new IllegalArgumentException("Not a CSS identifier: '" + value + "'");
    }
  }

  /// A plain name, such as `metric-card`. Throws IllegalArgumentException for a dashed name, for
  /// `none`, `default`, or a CSS-wide keyword in any letter case, and for text that is not an
  /// identifier.
  static CssIdent of(String name) {
    var ident = new CssIdent(name);
    if (ident.value.startsWith("--")) {
      throw new IllegalArgumentException("Pass the name without a leading '--': '" + name + "'");
    }
    if (RESERVED.contains(name.toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException("'" + name + "' is a CSS keyword, not a name");
    }
    return ident;
  }

  /// The dashed identifier `--name`. Throws IllegalArgumentException if `name` is empty, starts
  /// with `-`, or has characters outside `[A-Za-z0-9_-]`.
  static CssIdent custom(String name) {
    if (name.startsWith("-")) {
      throw new IllegalArgumentException("Pass the name without a leading '--': '" + name + "'");
    }
    return new CssIdent("--" + name);
  }

  /// `var(--name)` of a dashed identifier.
  String varReference() {
    return "var(" + value + ")";
  }

  @Override
  public String toCss() {
    return value;
  }
}
