package dev.nthings.tessera.render;

/// Text file produced while painting, such as a client behavior module. The `path` is under the
/// [OutputMode.ExternalAsset] base path, so it begins with `/` when that base is absolute; a
/// relative base yields a relative path. Site compilation requires an absolute base.
public record CompiledAsset(String path, String content) {}
