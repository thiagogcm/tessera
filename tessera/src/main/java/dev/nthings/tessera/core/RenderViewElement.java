package dev.nthings.tessera.core;

import org.jspecify.annotations.Nullable;

/// Holds a [RenderView] until the compiler expands it. Created by [RenderView#intoElement()], not
/// constructed by authors.
public record RenderViewElement(RenderView view, @Nullable SourceLocation source)
    implements Element {}
