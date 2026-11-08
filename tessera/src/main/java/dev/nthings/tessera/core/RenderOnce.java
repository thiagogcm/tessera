package dev.nthings.tessera.core;

/// Stateless component, typically a record. Expanded with an explicit render context.
@FunctionalInterface
public interface RenderOnce extends IntoElement {

  Element render(RenderContext context);

  @Override
  default Element intoElement() {
    return new RenderOnceElement(this, SourceProvenance.capture());
  }
}
