package dev.nthings.tessera.wasm;

/// A keyboard browser event such as `keydown` or `keyup`.
public sealed interface KeyboardEvent extends BrowserEvent permits NativeEvent.Keyboard {

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

  /// Whether the key belongs to an input method (IME) composition, such as the Enter that confirms
  /// a candidate. Skip shortcuts while it is `true`, so confirming text does not also submit or
  /// navigate.
  boolean composing();
}
