package dev.nthings.tessera.core;

import java.util.function.Function;

/// Content resolved after the page body is expanded, before document-end sections and head
/// contributions. `content` runs in the [RenderContext] where the element appears, and it can read
/// registries filled anywhere in the body, such as [RenderContext#headings()]. Late elements
/// resolve first in, first out. Create one with [Elements#late(Function)].
public record LateElement(Function<RenderContext, ? extends IntoElement> content)
    implements Element {}
