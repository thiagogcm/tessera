package dev.nthings.tessera.wasm;

/// A mouse, wheel, or pointer browser event, including `click`.
public sealed interface PointerEvent extends BrowserEvent permits NativeEvent.Pointer {

  /// The button that changed state: 0 primary, 1 auxiliary, 2 secondary.
  int button();

  /// Horizontal viewport coordinate in CSS pixels.
  double clientX();

  /// Vertical viewport coordinate in CSS pixels.
  double clientY();

  /// Click count for click events; 0 for other pointer events.
  int detail();

  boolean altKey();

  boolean ctrlKey();

  boolean metaKey();

  boolean shiftKey();
}
