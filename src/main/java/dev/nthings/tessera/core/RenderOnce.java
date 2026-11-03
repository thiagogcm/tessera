package dev.nthings.tessera.core;

/**
 * Stateless component contract, typically implemented by Java 25 {@code record} types.
 *
 * <p>Consumed during Phase 1 ({@code requestLayout}); contextual tokens and view scope are accessed
 * via {@link RenderContext#current()}.
 */
@FunctionalInterface
public interface RenderOnce extends IntoElement {

  Element render();

  @Override
  default Element intoElement() {
    return new RenderOnceElement(this);
  }
}
