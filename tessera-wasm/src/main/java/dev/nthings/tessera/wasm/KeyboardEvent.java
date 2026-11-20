package dev.nthings.tessera.wasm;

/// A keyboard browser event such as `keydown` or `keyup`.
public sealed interface KeyboardEvent extends BrowserEvent
    permits DomReconciler.NativeKeyboardEvent {

  /// The produced key value, such as `Enter`, `Escape`, or `a`.
  String key();

  /// The physical key code, such as `KeyA` or `ArrowRight`.
  String code();

  boolean altKey();

  boolean ctrlKey();

  boolean metaKey();

  boolean shiftKey();

  /// Whether the key is auto-repeating while held down.
  boolean repeat();
}
