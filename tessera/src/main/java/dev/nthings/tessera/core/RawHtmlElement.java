package dev.nthings.tessera.core;

/// Pre-sanitized HTML or inline SVG, emitted verbatim at paint.
public record RawHtmlElement(String trustedHtml) implements Element {}
