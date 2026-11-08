package dev.nthings.tessera.core;

import java.util.function.Function;

/// Placeholder resolved after the rest of the page's layout, in the [RenderContext] where it
/// appears, so `content` can read registries filled by elements placed anywhere on the page.
public record LateElement(Function<RenderContext, ? extends IntoElement> content)
    implements Element {}
