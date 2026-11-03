package dev.nthings.tessera.core;

/** Universal conversion contract for any value that can be lowered into an {@link Element}. */
@FunctionalInterface
public interface IntoElement {

  Element intoElement();
}
