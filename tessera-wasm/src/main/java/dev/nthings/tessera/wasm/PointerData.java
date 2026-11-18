package dev.nthings.tessera.wasm;

/// The data of a mouse or pointer event, clicks included, copied from [BrowserEvent#pointer()]: the
/// `button` that changed state (0 primary, 1 auxiliary, 2 secondary), the position in viewport
/// coordinates, the click count `detail` (0 for events other than clicks) and the modifier keys
/// held.
public record PointerData(
    int button,
    double clientX,
    double clientY,
    int detail,
    boolean altKey,
    boolean ctrlKey,
    boolean metaKey,
    boolean shiftKey) {}
