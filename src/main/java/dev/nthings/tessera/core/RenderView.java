package dev.nthings.tessera.core;

/**
 * Scoped view boundary representing a persistent domain entity with isolated {@code @scope} styles
 * and a dedicated {@link GlobalElementId} namespace.
 */
public interface RenderView extends IntoElement {

  ElementId entityId();

  default String viewScopeRule() {
    return "";
  }

  Element render();

  @Override
  default Element intoElement() {
    return new RenderViewElement(this);
  }
}
