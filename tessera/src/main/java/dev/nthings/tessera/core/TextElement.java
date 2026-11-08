package dev.nthings.tessera.core;

/// Text node, HTML-escaped with [org.owasp.encoder.Encode#forHtmlContent] at paint.
public record TextElement(String content) implements Element {}
