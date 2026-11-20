package dev.nthings.tessera.wasm;

/// A native browser event passed synchronously to a handler bound with [LiveElement#on].
///
/// The event is valid only while the handler runs: [#preventDefault()] and propagation calls take
/// effect immediately, and user-initiated handlers run within browser user activation (for example,
/// opening a modal dialog). Copy any values you need to retain beyond the callback.
public sealed interface BrowserEvent permits BasicEvent, PointerEvent, KeyboardEvent, ControlEvent {

  /// The native event type name, such as `click`, `input`, or `keydown`.
  String type();

  /// Cancels the browser's default action, such as following a link or submitting a form. Has no
  /// effect in a [EventOption#PASSIVE] listener.
  void preventDefault();

  /// Whether the default action is cancelled. Stays `false` in a [EventOption#PASSIVE] listener.
  boolean defaultPrevented();

  /// Stops the event from reaching elements further along its propagation path.
  void stopPropagation();

  /// Stops propagation and skips remaining listeners on the current element.
  void stopImmediatePropagation();
}
