package dev.nthings.tessera.wasm;

/// Native listener options for [LiveElement#on(EventType, java.util.function.Consumer,
/// EventOption...)].
public enum EventOption {
  /// Listen during the capture phase instead of the target and bubble phases.
  CAPTURE,
  /// Promise not to call [BrowserEvent#preventDefault()], allowing scrolling without waiting.
  PASSIVE,
  /// Dispatch at most once while the listener stays bound to its element.
  ONCE
}
