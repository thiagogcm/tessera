package dev.nthings.tessera.core;

import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.foundation.Fnv1a64;
import org.jspecify.annotations.Nullable;

/// Reusable component with its own id namespace and scoped CSS. Each instance opens a view scope
/// nested in the one it is placed in: [ElementRef#scoped(String)] refs bound inside get the DOM id
/// `<view id>--<name>`. The compiler marks the root element of the output with `data-view` and
/// `data-view-id`, and applies the styles from [#configureScope(ScopeStyle)] as an `@scope` block.
/// Use [Component] for markup that needs no scope of its own.
public interface View extends IntoElement {

  /// Instance id in the enclosing view scope. Characters outside `[A-Za-z0-9_-]` become `-` and
  /// runs of `-` collapse to one; an empty id throws IllegalArgumentException during compilation.
  /// `null` (the default) makes the compiler assign `<viewType>-<n>`, numbered per scope. Give
  /// repeated instances distinct ids within one enclosing scope: two instances with one id share a
  /// view scope, so their scoped element ids collide (`duplicate-id`).
  default @Nullable String viewId() {
    return null;
  }

  /// Name for `data-view` and the `@scope` block. The default is the kebab-cased simple class name,
  /// so `MetricCard` gives `metric-card`. Anonymous classes and lambdas have no stable class name:
  /// the compiler reports `unstable-view-type` for them unless they override this method.
  default String viewType() {
    return ViewTypes.DEFAULT_NAMES.get(getClass());
  }

  /// Adds this view's styles through `scope`, usually `scope.root(...)` for its root element.
  /// Styles apply from the view root down to, not into, nested views. Identical styles across
  /// instances share one `@scope` block.
  default void configureScope(ScopeStyle scope) {}

  /// Builds the view's content, which may be another component. The root element carries
  /// `data-view` and `data-view-id`. A result that is not a single element, such as text, a
  /// fragment or nothing, is wrapped in a `<div>`.
  IntoElement render(RenderContext context);

  @Override
  default Element intoElement() {
    return new ViewElement(this, SourceProvenance.capture());
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
