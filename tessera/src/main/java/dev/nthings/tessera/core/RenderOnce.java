package dev.nthings.tessera.core;

/// Stateless component, typically a record, for composition without a view scope: its ids live in
/// the enclosing [ViewScope]. Use [RenderView] for a reusable component that needs its own
/// namespace or isolated CSS.
@FunctionalInterface
public interface RenderOnce extends IntoElement {

  /// Builds the component's element. The compiler calls this for each place the component appears.
  /// Don't keep `context` after the call returns.
  Element render(RenderContext context);

  @Override
  default Element intoElement() {
    return new RenderOnceElement(this, SourceProvenance.capture());
  }
}
