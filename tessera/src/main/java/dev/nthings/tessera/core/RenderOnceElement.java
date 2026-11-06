package dev.nthings.tessera.core;

/// Wrapper element holding a lazy [RenderOnce] component prior to Phase 1 expansion.
public record RenderOnceElement(RenderOnce component) implements Element {}
