package dev.nthings.tessera.core;

/// Escaped text node. Content is streamed through [Encode#forHtmlContent] during the paint phase.
public record TextElement(String content) implements Element {}
