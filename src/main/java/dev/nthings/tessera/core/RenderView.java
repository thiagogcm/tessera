package dev.nthings.tessera.core;

import dev.nthings.tessera.css.ScopeStyle;

/// Scoped view boundary representing a persistent domain entity with isolated `@scope` styles and
/// a dedicated [GlobalElementId] namespace.
///
/// - [#viewType()] identifies the component type for `data-view` and deduplicates identical
///   `@scope` blocks across all instances of the view.
/// - [#entityId()] identifies the specific instance for [GlobalElementId] path folding and `data-
///   view-id`.
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
