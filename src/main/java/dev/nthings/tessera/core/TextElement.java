package dev.nthings.tessera.core;

import org.owasp.encoder.Encode;

/**
 * Escaped text node. Content is streamed through {@link Encode#forHtmlContent} during the paint
 * phase.
 */
public record TextElement(String content) implements Element {}
