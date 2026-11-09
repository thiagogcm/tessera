package dev.nthings.tessera.core;

import org.jspecify.annotations.Nullable;

/// Compiler-facing: holds a [Scope] until the compiler expands it. Created by
/// [Scope#intoElement()].
public record ScopeElement(Scope scope, @Nullable SourceLocation source) implements Element {}
