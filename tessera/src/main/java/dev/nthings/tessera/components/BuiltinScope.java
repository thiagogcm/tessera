package dev.nthings.tessera.components;

import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.foundation.FluentBuilder;
import org.jspecify.annotations.Nullable;

/// Base of the built-in components with an id scope. Placing one wraps it in a private [Scope]
/// that delegates to the package-private protocol methods below, so `expand`, `configureScope`
/// and `scopeName` stay off the public component types.
///
/// @param <B> the concrete component type, returned by the setters for chaining
abstract class BuiltinScope<B extends BuiltinScope<B>> implements IntoElement, FluentBuilder<B> {

  /// Instance id set through [#id(String)]; `null` lets the compiler assign `<scopeName>-<n>`.
  @Nullable String id;

  /// Names this instance in the enclosing id scope; ids bound inside it are prefixed with it, as
  /// in `<id>--name`. Without it, the compiler assigns `<scopeName>-<n>`, numbered per enclosing
  /// scope; each component's documentation names its scope name. Characters outside
  /// `[A-Za-z0-9_-]` become `-` and runs of `-` collapse to one. Give repeated instances in one
  /// scope distinct ids, or their scoped ids collide (`duplicate-id`).
  public B id(String id) {
    this.id = id;
    return self();
  }

  /// The stable `data-scope` name and `@scope` key of this component.
  abstract String scopeName();

  /// Adds the component's scoped styles.
  void configureScope(ScopeStyle scope) {}

  /// Builds the component's content; called by the compiler for each placement.
  abstract IntoElement expand(DocumentContext context);

  @Override
  public Element intoElement() {
    return new Scoped(this).intoElement();
  }

  private record Scoped(BuiltinScope<?> component) implements Scope {

    @Override
    public @Nullable String scopeId() {
      return component.id;
    }

    @Override
    public String scopeName() {
      return component.scopeName();
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      component.configureScope(scope);
    }

    @Override
    public IntoElement expand(DocumentContext context) {
      return component.expand(context);
    }
  }
}
