package dev.nthings.tessera.core;

/// A native browser event, passed to the handlers bound with [TagElement#onEvent]. It is valid only
/// while the handler runs: the handler is called synchronously from the browser's dispatch, so
/// [#preventDefault()] and the propagation methods take effect before the browser continues, and
/// the handler runs within the event's user activation (it may open a modal dialog or a popup).
/// Copy what you need with [#control()], [#keyboard()] or [#pointer()]; don't keep the event.
public interface BrowserEvent {

  /// The event type, such as `click` or `input`.
  String type();

  /// The current state of the input, textarea or select the handler is bound to. Throws
  /// IllegalStateException if the handler is bound to another element.
  ControlState control();

  /// The keyboard data of a keyboard event. Throws IllegalStateException for any other event.
  KeyboardData keyboard();

  /// The pointer data of a mouse or pointer event, clicks included. Throws IllegalStateException
  /// for any other event.
  PointerData pointer();

  /// Cancels the browser's default action, such as following a link or submitting a form. Has no
  /// effect in a passive listener.
  void preventDefault();

  /// Whether the default action is cancelled. Stays `false` in a passive listener.
  boolean defaultPrevented();

  /// Stops the event from reaching elements further along its propagation path.
  void stopPropagation();

  /// Stops propagation and skips the remaining listeners on the current element.
  void stopImmediatePropagation();
}
