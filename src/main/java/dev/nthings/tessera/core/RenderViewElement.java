package dev.nthings.tessera.core;

/** Wrapper element holding a scoped {@link RenderView} boundary prior to Phase 1 expansion. */
public record RenderViewElement(RenderView view) implements Element {}
