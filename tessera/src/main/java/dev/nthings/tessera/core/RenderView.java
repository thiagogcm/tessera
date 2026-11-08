package dev.nthings.tessera.core;

import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.foundation.Fnv1a64;
import org.jspecify.annotations.Nullable;

/// Reusable component with its own view scope and isolated CSS. The compiler opens a [ViewScope]
/// for each instance, marks the root element of its output with `data-view` and `data-view-id`,
/// and applies the styles from [#configureScope(ScopeStyle)] as an `@scope` block. Use [RenderOnce]
/// for markup that needs no scope of its own.
public interface RenderView extends IntoElement {

  /// Instance id in the enclosing view scope. `null` makes the compiler assign `<viewType>-<n>`,
  /// numbered per scope. Give repeated instances distinct ids within the same enclosing scope: two
  /// instances with one id share a view scope, so their explicit element ids collide.
  default @Nullable ElementId entityId() {
    return null;
  }

  /// Name for `data-view` and the `@scope` block. The default is the kebab-cased simple class name,
  /// so `MetricCard` gives `metric-card`, and anonymous classes give `view`. A lambda gets a
  /// generated name that changes between runs, so implement the view as a named class or record,
  /// or override this method.
  default String viewType() {
    return ViewTypes.DEFAULT_NAMES.get(getClass());
  }

  /// Adds this view's styles through `scope`, usually `scope.root(...)` for its root element.
  /// Styles apply from the view root down to, not into, nested views. Identical styles across
  /// instances share one `@scope` block.
  default void configureScope(ScopeStyle scope) {}

  /// Builds the view's content. The root element carries `data-view` and `data-view-id`. A result
  /// that is not a single element, such as text, a fragment or nothing, is wrapped in a `<div>`.
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
