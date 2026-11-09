package dev.nthings.tessera.core;

import org.jspecify.annotations.Nullable;

/// Compiler-facing: holds a [Component] until the compiler expands it. Created by
/// [Component#intoElement()]. `name` labels the component in diagnostic locations: the simple
/// class name, or `Component` for a lambda or anonymous class.
public record ComponentElement(Component component, String name, @Nullable SourceLocation source)
    implements Element {}
