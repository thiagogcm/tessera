package dev.nthings.tessera.core;

import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.internal.authoring.ClassNames;
import org.jspecify.annotations.Nullable;

/// The identity and styles shared by every styled scope family: portable [Scope], `DocumentScope`
/// and the browser module's live scope. A scope opens an id scope nested in the one it is placed
/// in: [ElementRef#scoped(String)] refs bound inside get the DOM id `<scope id>--<name>`. Its
/// explicit host element carries `data-scope` and `data-scope-id`, and the styles from
/// [#configureScope(ScopeStyle)] apply as an `@scope` block. Implement one of the family
/// interfaces, not this one: it places nothing by itself.
public interface ScopeDefinition {

  /// Instance id in the enclosing id scope. Characters outside `[A-Za-z0-9_-]` become `-` and runs
  /// of `-` collapse to one; an empty id throws IllegalArgumentException during expansion. `null`
  /// (the default) assigns `<scopeName>-<n>`, numbered per scope. Give repeated instances distinct
  /// ids within one enclosing scope: two instances with one id share an id scope, so their scoped
  /// element ids collide (`duplicate-id`).
  default @Nullable String scopeId() {
    return null;
  }

  /// Name for `data-scope` and the `@scope` block. The default is the kebab-cased simple class
  /// name, so `MetricCard` gives `metric-card`. Anonymous classes and lambdas have no stable name:
  /// expansion reports `unstable-scope-name` for them unless they override this method.
  default String scopeName() {
    return ClassNames.defaultScopeName(getClass());
  }

  /// Adds this scope's styles through `scope`, usually `scope.root(...)` for its host element.
  /// Styles apply from the scope host down to, not into, nested scopes. Identical styles across
  /// instances share one `@scope` block.
  default void configureScope(ScopeStyle scope) {}
}
