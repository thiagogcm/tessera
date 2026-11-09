package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.foundation.FluentBuilder;
import org.jspecify.annotations.Nullable;

/// Base of the built-in components with a view scope. Placing one wraps it in a private [View]
/// that delegates to the package-private protocol methods below, so `render`, `configureScope`
/// and `viewType` stay off the public component types.
///
/// @param <B> the concrete component type, returned by the setters for chaining
abstract class BuiltinView<B extends BuiltinView<B>> implements IntoElement, FluentBuilder<B> {

  /// Instance id set through [#id(String)]; `null` lets the compiler assign `<viewType>-<n>`.
  @Nullable String id;

  /// Names this instance in the enclosing view scope; ids bound inside it are prefixed with it, as
  /// in `<id>--name`. Without it, the compiler assigns `<view type>-<n>`, numbered per enclosing
  /// scope; each component's documentation names its view type. Characters outside
  /// `[A-Za-z0-9_-]` become `-` and runs of `-` collapse to one. Give repeated instances in one
  /// scope distinct ids, or their scoped ids collide (`duplicate-id`).
  public B id(String id) {
    this.id = id;
    return self();
  }

  /// The stable `data-view` name and `@scope` key of this component.
  abstract String viewType();

  /// Adds the component's scoped styles.
  void configureScope(ScopeStyle scope) {}

  /// Builds the component's content; called by the compiler for each placement.
  abstract IntoElement render(RenderContext context);

  @Override
  public Element intoElement() {
    return new Scoped(this).intoElement();
  }

  private record Scoped(BuiltinView<?> component) implements View {

    @Override
    public @Nullable String viewId() {
      return component.id;
    }

    @Override
    public String viewType() {
      return component.viewType();
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      component.configureScope(scope);
    }

    @Override
    public IntoElement render(RenderContext context) {
      return component.render(context);
    }
  }
}
