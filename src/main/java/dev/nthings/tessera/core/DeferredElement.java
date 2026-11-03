package dev.nthings.tessera.core;

/**
 * Wraps a child element to be traversed in its lexical {@link RenderContext} during Phase 1 and
 * hoisted to the end of the document body in priority order during Phase 2.
 */
public record DeferredElement(Element child, int priority) implements Element {

  public DeferredElement {
    if (priority < 0) {
      throw new IllegalArgumentException("Deferred priority must be >= 0");
    }
  }
}
