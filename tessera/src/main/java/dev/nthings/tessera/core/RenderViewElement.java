package dev.nthings.tessera.core;

import org.jspecify.annotations.Nullable;

/// Holds a [RenderView] boundary until Phase 1 expands it.
public record RenderViewElement(RenderView view, @Nullable SourceLocation source)
    implements Element {}
