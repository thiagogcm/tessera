package dev.nthings.tessera.markdown;

/// A fenced code block handed to a renderer registered with [Markdown#withFence(String,
/// java.util.function.Function)].
///
/// @param language the first word of the info string, such as `mermaid`
/// @param info the whole info string, such as `js title="app.js"`
/// @param code the block's content, without its final line break
public record Fence(String language, String info, String code) {}
