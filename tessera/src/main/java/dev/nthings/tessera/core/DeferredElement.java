package dev.nthings.tessera.core;

/// Child hoisted to the end of the body in priority order during Phase 2. Traversed in its
/// lexical [RenderContext] during Phase 1.
public record DeferredElement(Element child, int priority) implements Element {}
