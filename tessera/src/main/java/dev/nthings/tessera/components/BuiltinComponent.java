package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.internal.authoring.PortableNode;

/// Base of the built-in components that need no id scope. Placing one wraps its
/// [#expand(ViewContext)] in a [dev.nthings.tessera.core.Component], so expansion stays off the
/// public types. Diagnostics name it by its simple class name.
abstract class BuiltinComponent implements IntoElement {

  /// Builds the component's content; called by the compiler for each placement.
  abstract IntoElement expand(ViewContext context);

  @Override
  public Element intoElement() {
    return PortableNode.component(getClass(), this::expand);
  }
}
