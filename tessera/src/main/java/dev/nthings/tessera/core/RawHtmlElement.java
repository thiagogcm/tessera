package dev.nthings.tessera.core;

/// Trusted HTML or inline SVG, emitted verbatim. Tessera neither sanitizes nor validates it, so
/// pass only markup you control, never user input. Create one with [Elements#rawHtml(String)].
public record RawHtmlElement(String trustedHtml) implements Element {}
