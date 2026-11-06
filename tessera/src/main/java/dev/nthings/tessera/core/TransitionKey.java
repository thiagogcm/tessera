package dev.nthings.tessera.core;

import dev.nthings.tessera.css.CssIdent;
import dev.nthings.tessera.foundation.Fnv1a64;
import org.jspecify.annotations.Nullable;

/// Typed identity shared by counterpart elements across routes for Cross-Document View
/// Transitions.
///
/// Applying the same key to an element on two routes emits a matching `view-transition-name`, so
/// the browser morphs one into the other during navigation. The optional `transitionClass` emits
/// `view-transition-class` and receives shared `::view-transition-group(*.<class>)` timing rules.
///
/// Compile-time invariants: a key appears at most once per page (`PageCompiler`) and on at least
/// two routes site-wide (`SiteCompiler`).
public record TransitionKey(String name, @Nullable String transitionClass) {

  public TransitionKey {
    name = Fnv1a64.sanitizeIdentifier(name);
    if ("root".equals(name) || "none".equals(name) || "auto".equals(name)) {
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
