package dev.nthings.tessera.core;

/// Sealed algebraic sum type representing all nodes in a Tessera view tree.
public sealed interface Element extends IntoElement
    permits TagElement,
        FragmentElement,
        TextElement,
        RawHtmlElement,
        DeferredElement,
        LateElement,
        EmptyElement,
        RenderOnceElement,
        RenderViewElement {

  @Override
  default Element intoElement() {
    return this;
  }
}
