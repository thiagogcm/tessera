package dev.nthings.tessera.wasm;

/// The data of a keyboard event copied from [BrowserEvent#keyboard()]: the produced `key` (such as
/// `Enter` or `a`), the physical `code` (such as `KeyA`), the modifier keys held, and whether the
/// key is auto-repeating.
public record KeyboardData(
    String key,
    String code,
    boolean altKey,
    boolean ctrlKey,
    boolean metaKey,
    boolean shiftKey,
    boolean repeat) {}
