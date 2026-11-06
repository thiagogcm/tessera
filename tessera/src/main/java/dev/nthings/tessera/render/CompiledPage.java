package dev.nthings.tessera.render;

import org.jspecify.annotations.Nullable;

/// Immutable HTML and stylesheet output of page compilation.
public record CompiledPage(String html, @Nullable String externalCssPath, String cssContent) {}
