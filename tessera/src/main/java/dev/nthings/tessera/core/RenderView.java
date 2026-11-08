package dev.nthings.tessera.core;

import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.foundation.Fnv1a64;
import org.jspecify.annotations.Nullable;

/// Scoped view boundary for a persistent domain entity, with isolated `@scope` styles and a
/// [ViewScope] namespace.
///
/// - [#viewType()] names the `data-view` and dedupes identical `@scope` blocks across instances.
/// - [#entityId()] names the instance in [ViewScope] folding and `data-view-id`.
public interface RenderView extends IntoElement {

  /// Instance id in the enclosing view scope; `null` makes the compiler assign `<viewType>-<n>`.
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
    return new RenderViewElement(this, SourceProvenance.capture());
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
