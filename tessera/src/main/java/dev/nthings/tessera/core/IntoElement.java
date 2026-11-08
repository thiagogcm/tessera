package dev.nthings.tessera.core;

/// Any value that can be lowered into an [Element].
@FunctionalInterface
public interface IntoElement {

  Element intoElement();
}
