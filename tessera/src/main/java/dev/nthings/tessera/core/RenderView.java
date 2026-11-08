package dev.nthings.tessera.core;

import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.foundation.Fnv1a64;
import org.jspecify.annotations.Nullable;

/// Scoped view boundary representing a persistent domain entity with isolated `@scope` styles and
/// a dedicated [ViewScope] namespace.
///
/// - [#viewType()] identifies the component type for `data-view` and deduplicates identical
///   `@scope` blocks across all instances of the view.
/// - [#entityId()] identifies the specific instance for [ViewScope] path folding and
///   `data-view-id`.
public interface RenderView extends IntoElement {

  /// Instance id within the enclosing view scope; `null` lets the compiler assign
  /// `<viewType>-<n>` deterministically per page.
  default @Nullable ElementId entityId() {
    return null;
  }

  default String viewType() {
    return ViewTypes.DEFAULT_NAMES.get(getClass());
  }

  default void configureScope(ScopeStyle scope) {}

  Element render(RenderContext context);

  @Override
  default Element intoElement() {
    return new RenderViewElement(this, SourceTracking.capture());
  }
}

final class ViewTypes {
  static final ClassValue<String> DEFAULT_NAMES =
      new ClassValue<>() {
        @Override
        protected String computeValue(Class<?> type) {
          return Fnv1a64.toKebabCase(type.getSimpleName());
        }
      };

  private ViewTypes() {}
}
