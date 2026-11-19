package dev.nthings.tessera.internal.authoring;

import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.SourceLocation;
import org.jspecify.annotations.Nullable;

/// Closed portable placements that do not expose a builder API.
public sealed interface PortableNode {

  enum Empty implements PortableNode {
    INSTANCE
  }

  record Text(String content) implements PortableNode {}

  record Keyed(String key, Element content) implements PortableNode {
    public Keyed {
      requireKey(key);
    }
  }

  record ComponentPlacement(Component component, Class<?> type, @Nullable SourceLocation source)
      implements PortableNode {}

  record ScopePlacement(Scope scope, @Nullable SourceLocation source) implements PortableNode {}

  static void requireKey(String key) {
    if (key.isBlank()) {
      throw new IllegalArgumentException("A sibling key must not be blank");
    }
  }
}
