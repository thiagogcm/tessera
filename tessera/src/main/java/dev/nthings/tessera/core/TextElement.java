package dev.nthings.tessera.core;

/// Text node. Its content is HTML-escaped on output, so it never becomes markup. Create one with
/// [Elements#text(String)] or a builder's `text(...)` method.
public record TextElement(String content) implements Element {}
