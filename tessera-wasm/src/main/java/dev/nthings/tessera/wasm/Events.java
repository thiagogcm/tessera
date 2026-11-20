package dev.nthings.tessera.wasm;

import dev.nthings.tessera.wasm.EventType.Source;

/// Typed browser event keys for [LiveElement#on].
public final class Events {

  private Events() {}

  // --- Pointer and mouse events -------------------------------------------------------------

  public static final EventType<PointerEvent> CLICK = new EventType<>("click", Source::pointer);
  public static final EventType<PointerEvent> DBLCLICK =
      new EventType<>("dblclick", Source::pointer);
  public static final EventType<PointerEvent> CONTEXTMENU =
      new EventType<>("contextmenu", Source::pointer);
  public static final EventType<PointerEvent> POINTERDOWN =
      new EventType<>("pointerdown", Source::pointer);
  public static final EventType<PointerEvent> POINTERUP =
      new EventType<>("pointerup", Source::pointer);
  public static final EventType<PointerEvent> POINTERMOVE =
      new EventType<>("pointermove", Source::pointer);
  public static final EventType<PointerEvent> POINTERENTER =
      new EventType<>("pointerenter", Source::pointer);
  public static final EventType<PointerEvent> POINTERLEAVE =
      new EventType<>("pointerleave", Source::pointer);
  public static final EventType<PointerEvent> MOUSEDOWN =
      new EventType<>("mousedown", Source::pointer);
  public static final EventType<PointerEvent> MOUSEUP = new EventType<>("mouseup", Source::pointer);
  public static final EventType<PointerEvent> MOUSEMOVE =
      new EventType<>("mousemove", Source::pointer);
  public static final EventType<PointerEvent> WHEEL = new EventType<>("wheel", Source::pointer);

  // --- Keyboard events ----------------------------------------------------------------------

  public static final EventType<KeyboardEvent> KEYDOWN =
      new EventType<>("keydown", Source::keyboard);
  public static final EventType<KeyboardEvent> KEYUP = new EventType<>("keyup", Source::keyboard);

  // --- Form control events ------------------------------------------------------------------

  public static final EventType<ControlEvent> INPUT = new EventType<>("input", Source::control);
  public static final EventType<ControlEvent> CHANGE = new EventType<>("change", Source::control);

  // --- Form, focus, overlay, and media events -----------------------------------------------

  public static final EventType<BasicEvent> SUBMIT = new EventType<>("submit", Source::basic);
  public static final EventType<BasicEvent> RESET = new EventType<>("reset", Source::basic);
  public static final EventType<BasicEvent> FOCUS = new EventType<>("focus", Source::basic);
  public static final EventType<BasicEvent> BLUR = new EventType<>("blur", Source::basic);
  public static final EventType<BasicEvent> FOCUSIN = new EventType<>("focusin", Source::basic);
  public static final EventType<BasicEvent> FOCUSOUT = new EventType<>("focusout", Source::basic);
  public static final EventType<BasicEvent> CLOSE = new EventType<>("close", Source::basic);
  public static final EventType<BasicEvent> CANCEL = new EventType<>("cancel", Source::basic);
  public static final EventType<BasicEvent> TOGGLE = new EventType<>("toggle", Source::basic);
  public static final EventType<BasicEvent> PLAY = new EventType<>("play", Source::basic);
  public static final EventType<BasicEvent> PAUSE = new EventType<>("pause", Source::basic);
  public static final EventType<BasicEvent> ENDED = new EventType<>("ended", Source::basic);

  /// Creates an event key for a custom or unlisted native event `name`. Throws
  /// IllegalArgumentException if `name` is blank.
  public static EventType<BasicEvent> custom(String name) {
    return new EventType<>(name, Source::basic);
  }
}
