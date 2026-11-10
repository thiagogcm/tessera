package dev.nthings.tessera.wasm;

/// An audio or video element's playback state, copied when read. `currentTime` and `duration` are
/// in seconds; `duration` is `NaN` before the metadata loads.
public record MediaState(
    boolean paused,
    boolean ended,
    boolean muted,
    double currentTime,
    double duration,
    double volume) {}
