package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.css.ScopeStyle;
import org.jspecify.annotations.Nullable;

/// Base of the built-in components with a view scope. Placing one wraps it in a private [View]
/// that delegates to the package-private protocol methods below, so `render`, `configureScope`,
/// `viewType` and `viewId` stay off the public component types.
abstract class BuiltinView implements IntoElement {

  /// Instance id set through the component's public `id(String)`; `null` lets the compiler assign
  /// `<viewType>-<n>`.
  @Nullable String id;

  /// The stable `data-view` name and `@scope` key of this component.
  abstract String viewType();

  /// The instance id in the enclosing view scope, or `null` for an assigned one.
  @Nullable String viewId() {
    return id;
  }

  /// Adds the component's scoped styles.
  void configureScope(ScopeStyle scope) {}

  /// Builds the component's content; called by the compiler for each placement.
  abstract IntoElement render(RenderContext context);

  @Override
  public Element intoElement() {
    return new Scoped(this).intoElement();
  }

  private record Scoped(BuiltinView component) implements View {

    @Override
    public @Nullable String viewId() {
      return component.viewId();
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
