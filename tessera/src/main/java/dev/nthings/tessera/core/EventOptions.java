package dev.nthings.tessera.core;

/// Native listener options for [TagElement#onEvent]. `capture` listens during the capture phase
/// instead of the target and bubble phases; `passive` promises not to call `preventDefault()`, so
/// the browser need not wait for the handler before scrolling; `once` dispatches at most once
/// while the listener stays bound to its element. Start from [#DEFAULT], where all three are off,
/// and switch them on with the `with` methods.
public record EventOptions(boolean capture, boolean passive, boolean once) {

  /// Bubble phase, not passive, repeating: what [TagElement#onEvent(String,
  /// java.util.function.Consumer)] uses.
  public static final EventOptions DEFAULT = new EventOptions(false, false, false);

  /// These options, listening during the capture phase.
  public EventOptions withCapture() {
    return new EventOptions(true, passive, once);
  }

  /// These options, as a passive listener.
  public EventOptions withPassive() {
    return new EventOptions(capture, true, once);
  }

  /// These options, dispatching at most once.
  public EventOptions withOnce() {
    return new EventOptions(capture, passive, true);
  }
}
