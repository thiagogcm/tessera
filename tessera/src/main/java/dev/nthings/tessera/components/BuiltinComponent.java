package dev.nthings.tessera.components;

import dev.nthings.tessera.core.ComponentElement;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.SourceProvenance;

/// Base of the built-in components that need no id scope. Placing one wraps its
/// [#expand(DocumentContext)] in a [dev.nthings.tessera.core.Component], so expansion
/// stays off the public types. Diagnostics name it by its simple class name.
abstract class BuiltinComponent implements IntoElement {

  /// Builds the component's content; called by the compiler for each placement.
  abstract IntoElement expand(DocumentContext context);

  @Override
  public Element intoElement() {
    return new ComponentElement(this::expand, getClass(), SourceProvenance.capture());
  }
}
