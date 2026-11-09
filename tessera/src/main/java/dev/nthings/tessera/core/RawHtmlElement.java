package dev.nthings.tessera.core;

/// Compiler-facing: trusted HTML or inline SVG emitted verbatim, created by
/// [Elements#rawHtml(String)]. Tessera neither sanitizes nor validates it.
public record RawHtmlElement(String trustedHtml) implements Element {}
