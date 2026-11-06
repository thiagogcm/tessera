package dev.nthings.tessera.core;

/// Pre-sanitized HTML fragment or inline SVG node emitted verbatim during the paint phase.
public record RawHtmlElement(String trustedHtml) implements Element {}
