package dev.nthings.tessera.core;

import java.util.function.Function;

/// Resolved after the rest of the page is laid out, in the [RenderContext] where it appears,
/// so `content` can read registries filled by elements anywhere on the page.
public record LateElement(Function<RenderContext, ? extends IntoElement> content)
    implements Element {}
