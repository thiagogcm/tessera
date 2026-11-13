package dev.nthings.tessera.core;

import dev.nthings.tessera.css.ScopeStyle;
import org.jspecify.annotations.Nullable;

/// Reusable component with its own id namespace and scoped CSS. Each instance opens an id scope
/// nested in the one it is placed in: [ElementRef#scoped(String)] refs bound inside get the DOM id
/// `<scope id>--<name>`. The compiler marks the root element of the output with `data-scope` and
/// `data-scope-id`, and applies the styles from [#configureScope(ScopeStyle)] as an `@scope` block.
/// Use [Component] for markup that needs no scope of its own.
public interface Scope extends IntoElement {

  /// Instance id in the enclosing id scope. Characters outside `[A-Za-z0-9_-]` become `-` and
  /// runs of `-` collapse to one; an empty id throws IllegalArgumentException during compilation.
  /// `null` (the default) makes the compiler assign `<scopeName>-<n>`, numbered per scope. Give
  /// repeated instances distinct ids within one enclosing scope: two instances with one id share
  /// an id scope, so their scoped element ids collide (`duplicate-id`).
  default @Nullable String scopeId() {
    return null;
  }

  /// Name for `data-scope` and the `@scope` block. The default is the kebab-cased simple class
  /// name, so `MetricCard` gives `metric-card`. Anonymous classes and lambdas have no stable name:
  /// the compiler reports `unstable-scope-name` for them unless they override this method.
  default String scopeName() {
    return ClassNames.defaultScopeName(getClass());
  }

  /// Adds this scope's styles through `scope`, usually `scope.root(...)` for its root element.
  /// Styles apply from the scope root down to, not into, nested scopes. Identical styles across
  /// instances share one `@scope` block.
  default void configureScope(ScopeStyle scope) {}

  /// Builds the scope's content. The result must resolve to one element host, which carries
  /// `data-scope` and `data-scope-id`. A component that itself resolves to one element is a valid
  /// host. Returning another scope reports `scope-root` and leaves that scope's host alone. A
  /// fragment, text, raw HTML, late or deferred content, or nothing also reports `scope-root`.
  /// Expansion does not insert a wrapper. Place nested scopes inside the host: a list scope
  /// returns `ul` or `ol`, and a table-row scope returns `tr`.
  IntoElement expand(DocumentContext context);

  @Override
  default Element intoElement() {
    return new ScopeElement(this, SourceProvenance.capture());
  }
}
