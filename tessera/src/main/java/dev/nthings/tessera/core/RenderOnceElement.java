package dev.nthings.tessera.core;

import org.jspecify.annotations.Nullable;

/// Wrapper element holding a lazy [RenderOnce] component prior to Phase 1 expansion.
public record RenderOnceElement(RenderOnce component, @Nullable SourceLocation source)
    implements Element {}
