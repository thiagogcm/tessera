package dev.nthings.tessera.internal.authoring;

import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.SourceLocation;
import org.jspecify.annotations.Nullable;

/// Closed portable placements that do not expose a builder API.
public sealed interface PortableNode extends Element {

  enum Empty implements PortableNode {
    INSTANCE
  }

  record Text(String content) implements PortableNode {}

  record Keyed(String key, Element content) implements PortableNode {
    public Keyed {
      requireKey(key);
    }

    @Override
    public Element key(String key) {
      return new Keyed(key, content);
    }
  }

  record ComponentPlacement(Component component, Class<?> type, @Nullable SourceLocation source)
      implements PortableNode {}

  record ScopePlacement(Scope scope, @Nullable SourceLocation source) implements PortableNode {}

  static Element component(Class<?> type, Component component) {
    return new ComponentPlacement(component, type, SourceProvenance.capture());
  }

  static void requireKey(String key) {
    if (key.isBlank()) {
      throw new IllegalArgumentException("A sibling key must not be blank");
    }
  }
}
