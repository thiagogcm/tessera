package dev.nthings.tessera.core;

/// Stateless component contract, typically implemented by Java 25 `record` types.
///
/// Consumed during expansion with an explicit turn context.
@FunctionalInterface
public interface RenderOnce extends IntoElement {

  Element render(RenderContext context);

  @Override
  default Element intoElement() {
    return new RenderOnceElement(this, SourceProvenance.capture());
  }
}
