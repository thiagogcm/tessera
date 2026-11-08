package dev.nthings.tessera.core;

import dev.nthings.tessera.css.CssIdent;
import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.Locale;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// Identity shared by counterpart elements on different routes for cross-document view
/// transitions. The same key on two routes emits a matching `view-transition-name`, so the browser
/// morphs one element into the other. The optional `transitionClass` emits
/// `view-transition-class` and receives shared `::view-transition-group(*.<class>)` timing rules.
///
/// Rules: `duplicate-transition-key` reports a name used twice on one page.
/// `unshared-transition-key` reports a key that appears on fewer than two routes site-wide. Both
/// name and class are part of the key, so the class must match too. Attach a key with
/// [InteractiveElement#transitionKey(TransitionKey)].
public record TransitionKey(String name, @Nullable String transitionClass) {

  private static final Set<String> RESERVED = Set.of("root", "auto", "match-element");

  /// Sanitizes `name` and `transitionClass`: characters outside `[A-Za-z0-9_-]` become `-`. Throws
  /// IllegalArgumentException for an empty name, for `root`, `auto` or `match-element` in any case,
  /// for a name starting with a digit or `--`, and for CSS-wide keywords, `none` and `default`.
  public TransitionKey {
    name = Fnv1a64.sanitizeIdentifier(name);
    // Reserved names are case-insensitive; CssIdent rejects `none` and CSS-wide keywords.
    if (RESERVED.contains(name.toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException("TransitionKey name is reserved: " + name);
    }
    CssIdent.of(name);
    if (transitionClass != null) {
      transitionClass = Fnv1a64.sanitizeIdentifier(transitionClass);
      CssIdent.of(transitionClass);
    }
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
