package dev.nthings.tessera.render;

/// Non-page output produced while painting a page, such as an external client behavior module.
///
/// @param path published path, always beginning with `/`
/// @param contentType media type written with the asset
/// @param content owned bytes
public record CompiledAsset(String path, String contentType, byte[] content) {}
