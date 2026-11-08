package dev.nthings.tessera.core;

/// Any node in a Tessera view tree.
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
