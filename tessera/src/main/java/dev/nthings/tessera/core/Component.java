package dev.nthings.tessera.core;

import dev.nthings.tessera.internal.authoring.PortableNode;

/// Portable stateless component, typically a record or a lambda, for composition without an id
/// scope of its own: the ids it binds live in the enclosing scope. It receives only a [ViewContext]
/// and produces portable content, so the same component works in a static page and a browser root.
/// Use [Scope] for a reusable component that needs its own id namespace or scoped CSS, and
/// `DocumentComponent` for content that needs document assembly.
@FunctionalInterface
public interface Component extends IntoElement {

  /// Builds the component's content. Expansion calls this for each place the component appears, so
  /// the result may be another component. Don't keep `context` after the call returns.
  IntoElement expand(ViewContext context);

  @Override
  default Element intoElement() {
    return PortableNode.component(getClass(), this);
  }
}
