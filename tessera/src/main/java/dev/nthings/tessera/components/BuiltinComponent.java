package dev.nthings.tessera.components;

import dev.nthings.tessera.core.ComponentElement;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.SourceProvenance;

/// Base of the built-in components that need no view scope. Placing one wraps its package-private
/// [#render(RenderContext)] in a [dev.nthings.tessera.core.Component], so the render protocol stays
/// off the public component types. Diagnostics name it by its simple class name.
abstract class BuiltinComponent implements IntoElement {

  /// Builds the component's content; called by the compiler for each placement.
  abstract IntoElement render(RenderContext context);

  @Override
  public Element intoElement() {
    return new ComponentElement(
        this::render, getClass().getSimpleName(), SourceProvenance.capture());
  }
}
