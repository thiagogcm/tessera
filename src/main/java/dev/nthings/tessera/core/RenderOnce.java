package dev.nthings.tessera.core;

/// Stateless component contract, typically implemented by Java 25 `record` types.
///
/// Consumed during Phase 1 (`requestLayout`); contextual tokens and view scope are accessed via
/// [RenderContext#current()].
@FunctionalInterface
public interface RenderOnce extends IntoElement {

  Element render();

  @Override
  default Element intoElement() {
    return new RenderOnceElement(this);
  }
}
