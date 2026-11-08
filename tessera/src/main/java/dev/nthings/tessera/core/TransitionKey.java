package dev.nthings.tessera.core;

import dev.nthings.tessera.css.CssIdent;
import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.Locale;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// Identity shared by counterpart elements across routes for Cross-Document View Transitions.
///
/// The same key on two routes emits a matching `view-transition-name`, so the browser morphs one
/// into the other. The optional `transitionClass` emits `view-transition-class` and receives shared
/// `::view-transition-group(*.<class>)` timing rules.
///
/// Invariants: a key appears at most once per page (`PageCompiler`) and on at least two routes
/// site-wide (`SiteCompiler`).
public record TransitionKey(String name, @Nullable String transitionClass) {

  private static final Set<String> RESERVED = Set.of("root", "auto", "match-element");

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

  public static TransitionKey of(String name) {
    return new TransitionKey(name, null);
  }

  public static TransitionKey of(String name, String transitionClass) {
    return new TransitionKey(name, transitionClass);
  }
}
