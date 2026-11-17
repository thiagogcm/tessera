package dev.nthings.tessera.core;

import org.jspecify.annotations.Nullable;

/// Compiler-facing: holds a [Scope] until expansion. Created by [Scope#intoElement()]. A live
/// backend replaces that DOM when [Scope#scopeName()] at the position changes.
public record ScopeElement(Scope scope, @Nullable SourceLocation source) implements Element {}
