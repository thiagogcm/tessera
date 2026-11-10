package dev.nthings.tessera.core;

/// Backend-facing: content with a stable identity among its siblings, created by
/// [IntoElement#key(String)]. Shared expansion scopes the ids bound inside it by this key, and a
/// live backend reconciles the child by it. Throws IllegalArgumentException if `key` is blank.
public record KeyedElement(String key, Element content) implements Element {

  public KeyedElement {
    if (key.isBlank()) {
      throw new IllegalArgumentException("A sibling key must not be blank");
    }
  }

  /// Returns the same content under `key` instead, so the last key wins.
  @Override
  public Element key(String key) {
    return new KeyedElement(key, content);
  }
}
