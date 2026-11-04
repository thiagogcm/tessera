package dev.nthings.tessera.core;

import dev.nthings.tessera.css.ScopeStyle;

/**
 * Scoped view boundary representing a persistent domain entity with isolated {@code @scope} styles
 * and a dedicated {@link GlobalElementId} namespace.
 *
 * <ul>
 *   <li>{@link #viewType()} identifies the component type for {@code data-view} and deduplicates
 *       identical {@code @scope} blocks across all instances of the view.
 *   <li>{@link #entityId()} identifies the specific instance for {@link GlobalElementId} path
 *       folding and {@code data-view-id}.
 * </ul>
 */
public interface RenderView extends IntoElement {

  ElementId entityId();

  default String viewType() {
    String simple = getClass().getSimpleName();
    return simple.isEmpty() ? "view" : Fnv1a64.toKebabCase(simple);
  }

  default void configureScope(ScopeStyle scope) {}

  default String viewScopeRule() {
    var scope = new ScopeStyle();
    configureScope(scope);
    return scope.toCssBody();
  }

  Element render();

  @Override
  default Element intoElement() {
    return new RenderViewElement(this);
  }
}
