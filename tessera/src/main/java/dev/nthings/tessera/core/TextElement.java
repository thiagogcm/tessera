package dev.nthings.tessera.core;

/// Compiler-facing: text node, created by [Elements#text(String)] or a builder's `text(...)`
/// method. Its content is HTML-escaped on output, so it never becomes markup.
public record TextElement(String content) implements Element {}
