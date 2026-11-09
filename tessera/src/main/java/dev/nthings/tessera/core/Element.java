package dev.nthings.tessera.core;

/// Any node in a Tessera view tree. Authors build elements through [Elements] and get back
/// [TagElement], [FragmentElement] or plain `Element`; the other variants are compiler-facing.
public sealed interface Element extends IntoElement
    permits TagElement,
        FragmentElement,
        TextElement,
        RawHtmlElement,
        DeferredElement,
        LateElement,
        EmptyElement,
        ComponentElement,
        ViewElement,
        KeyedElement {

  @Override
  default Element intoElement() {
    return this;
  }
}
