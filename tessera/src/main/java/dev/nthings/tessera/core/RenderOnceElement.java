package dev.nthings.tessera.core;

import org.jspecify.annotations.Nullable;

/// Holds a [RenderOnce] until the compiler expands it. Created by [RenderOnce#intoElement()], not
/// constructed by authors.
public record RenderOnceElement(RenderOnce component, @Nullable SourceLocation source)
    implements Element {}
