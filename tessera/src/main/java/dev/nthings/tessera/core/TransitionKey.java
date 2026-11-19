package dev.nthings.tessera.core;

import dev.nthings.tessera.internal.foundation.Fnv1a64;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/// Identity shared by counterpart elements on different routes for cross-document view transitions.
/// The same key on two routes emits a matching `view-transition-name`, so the browser morphs one
/// element into the other. The optional `transitionClass` emits `view-transition-class` and
/// receives shared `::view-transition-group(*.<class>)` timing rules.
///
/// Rules: `duplicate-transition-key` reports a name used twice on one page.
/// `unshared-transition-key` reports a key that appears on fewer than two routes site-wide. Both
/// name and class are part of the key, so the class must match too. Attach a key with
/// [TagElement#transitionKey(TransitionKey)].
public record TransitionKey(String name, @Nullable String transitionClass) {

  private static final Set<String> RESERVED =
      Set.of(
          "root",
          "auto",
          "match-element",
          "none",
          "default",
          "initial",
          "inherit",
          "unset",
          "revert",
          "revert-layer");

  private static final Pattern IDENT = Pattern.compile("-?[A-Za-z_][A-Za-z0-9_-]*");

  /// Sanitizes `name` and `transitionClass`: characters outside `[A-Za-z0-9_-]` become `-`. Throws
  /// IllegalArgumentException for an empty name, for `root`, `auto` or `match-element` in any case,
  /// for a name starting with a digit or `--`, and for CSS-wide keywords, `none` and `default`.
  public TransitionKey {
    name = requireName(Fnv1a64.sanitizeIdentifier(name));
    if (transitionClass != null) {
      transitionClass = requireName(Fnv1a64.sanitizeIdentifier(transitionClass));
    }
  }

  /// Reserved names are case-insensitive.
  private static String requireName(String name) {
    if (RESERVED.contains(name.toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException("TransitionKey name is reserved: " + name);
    }
    if (!IDENT.matcher(name).matches()) {
      throw new IllegalArgumentException("Not a CSS identifier: '" + name + "'");
    }
    return name;
  }

  /// Creates a key without a transition class.
  public static TransitionKey of(String name) {
    return new TransitionKey(name, null);
  }

  /// Creates a key whose elements also share `transitionClass`.
  public static TransitionKey of(String name, String transitionClass) {
    return new TransitionKey(name, transitionClass);
  }
}
