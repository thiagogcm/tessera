package dev.nthings.tessera.wasm;

/// A form-control browser event (`input` or `change`) carrying the current [ControlState] of the
/// control the handler is bound to.
public sealed interface ControlEvent extends BrowserEvent permits NativeEvent.Control {

  /// Copies the current state of the `<input>`, `<textarea>`, or `<select>` the handler is bound
  /// to. Throws IllegalStateException if bound to another element.
  ControlState control();
}
