package dev.nthings.tessera.css;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/// A CSS identifier for names such as containers, anchors, counters, and view transitions. Use
/// [#of] for a plain name and [#custom] for a dashed identifier (`--name`), such as a custom
/// property.
public record CssIdent(String value) implements CssValue {

  private static final Pattern IDENT =
      Pattern.compile("--[A-Za-z0-9_-]+|-?[A-Za-z_][A-Za-z0-9_-]*");

  /// Rejected as names: the CSS-wide keywords, `default`, and `none`, which name-taking
  /// properties also accept as values.
  private static final Set<String> RESERVED =
      Set.of("initial", "inherit", "unset", "revert", "revert-layer", "default", "none");

  /// Checks identifier syntax only. Use [#of] or [#custom], which also apply the name rules.
  public CssIdent {
    if (!IDENT.matcher(value).matches()) {
      throw new IllegalArgumentException("Not a CSS identifier: '" + value + "'");
    }
  }

  /// A plain name, such as `metric-card`. Throws IllegalArgumentException for a dashed name, for
  /// `none`, `default`, or a CSS-wide keyword in any letter case, and for text that is not an
  /// identifier.
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

  /// A dashed identifier `--name`. Pass `name` without the leading `--`: `custom("x")` is `--x`,
  /// while `custom("--x")` is `----x`. Throws IllegalArgumentException if `name` is empty or has
  /// characters outside `[A-Za-z0-9_-]`.
  public static CssIdent custom(String name) {
    return new CssIdent("--" + name);
  }

  /// Whether this is a dashed identifier (`--name`).
  public boolean isCustom() {
    return value.startsWith("--");
  }

  /// Returns this identifier if it is dashed. Throws IllegalArgumentException otherwise.
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
