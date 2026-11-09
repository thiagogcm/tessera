package dev.nthings.tessera.core;

import org.jspecify.annotations.Nullable;

/// Compiler-facing: holds a [View] until the compiler expands it. Created by
/// [View#intoElement()].
public record ViewElement(View view, @Nullable SourceLocation source) implements Element {}
