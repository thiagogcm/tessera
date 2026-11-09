package dev.nthings.tessera.core;

/// Stateless component, typically a record or a lambda, for composition without a view scope of
/// its own: the ids it binds live in the enclosing scope. Use [View] for a reusable component that
/// needs its own id namespace or scoped CSS.
@FunctionalInterface
public interface Component extends IntoElement {

  /// Builds the component's content. The compiler calls this for each place the component appears,
  /// so the result may be another component. Don't keep `context` after the call returns.
  IntoElement render(RenderContext context);

  @Override
  default Element intoElement() {
    var type = getClass();
    var name = type.isSynthetic() || type.isAnonymousClass() ? "Component" : type.getSimpleName();
    return new ComponentElement(this, name, SourceProvenance.capture());
  }
}
