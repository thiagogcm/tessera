package dev.nthings.tessera.core;

import org.jspecify.annotations.Nullable;

/// Compiler-facing: holds a [Component] until the compiler expands it. Created by
/// [Component#intoElement()].
public record ComponentElement(Component component, @Nullable SourceLocation source)
    implements Element {}
