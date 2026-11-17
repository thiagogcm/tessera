package dev.nthings.tessera.wasm;

/// A text control's selection, copied when read: the `start` and `end` offsets and the `direction`,
/// which is `forward`, `backward` or `none`.
public record TextSelection(int start, int end, String direction) {}
