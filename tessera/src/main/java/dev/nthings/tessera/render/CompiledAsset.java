package dev.nthings.tessera.render;

/// Text file produced while painting, such as a client behavior module.
///
/// @param path published path, always beginning with `/`
/// @param content file contents
public record CompiledAsset(String path, String content) {}
