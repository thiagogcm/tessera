package dev.nthings.tessera.core;

import java.util.function.Function;

/// Compiler-facing: content resolved after the page body is expanded, created by
/// [Elements#late(Function)]. `content` runs in the [DocumentContext] where the element appears.
public record LateElement(Function<DocumentContext, ? extends IntoElement> content)
    implements Element {}
