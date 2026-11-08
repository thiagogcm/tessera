package dev.nthings.tessera.core;

import org.jspecify.annotations.Nullable;

/// Holds a [RenderOnce] until Phase 1 expands it.
public record RenderOnceElement(RenderOnce component, @Nullable SourceLocation source)
    implements Element {}
