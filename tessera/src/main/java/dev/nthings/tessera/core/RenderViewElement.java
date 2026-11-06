package dev.nthings.tessera.core;

import org.jspecify.annotations.Nullable;

/// Wrapper element holding a scoped [RenderView] boundary prior to Phase 1 expansion.
public record RenderViewElement(RenderView view, @Nullable SourceLocation source)
    implements Element {}
