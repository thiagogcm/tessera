package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.TagName;
import org.graalvm.webimage.api.JSObject;

/// The dispatched JavaScript event behind each [BrowserEvent] a handler receives. Accessors read
/// the native event when called, so a handler pays only for the properties it uses.
abstract sealed class NativeEvent
    permits NativeEvent.Basic, NativeEvent.Modified, NativeEvent.Control {
  final JSObject event;

  private NativeEvent(JSObject event) {
    this.event = event;
  }

  public final String type() {
    return Dom.eventString(event, "type");
  }

  public final void preventDefault() {
    Dom.preventDefault(event);
  }

  public final boolean defaultPrevented() {
    return Dom.flag(event, "defaultPrevented");
  }

  public final void stopPropagation() {
    Dom.stopPropagation(event);
  }

  public final void stopImmediatePropagation() {
    Dom.stopImmediatePropagation(event);
  }

  final boolean flag(String name) {
    return Dom.flag(event, name);
  }

  /// One dispatch of `event` to a handler bound on `target`, an element with tag `tag`. `composing`
  /// is whether the reconciler tracks an input method composition on `target`.
  record Source(JSObject event, JSObject target, TagName tag, boolean composing)
      implements EventType.Source {
    @Override
    public BasicEvent basic() {
      return new Basic(event);
    }

    @Override
    public PointerEvent pointer() {
      if (!Dom.isMouseEvent(event)) {
        throw new IllegalStateException(
            "Pointer data requires a mouse or pointer event, not "
                + Dom.eventString(event, "type"));
      }
      return new Pointer(event);
    }

    @Override
    public KeyboardEvent keyboard() {
      if (!Dom.isKeyboardEvent(event)) {
        throw new IllegalStateException(
            "Keyboard data requires a keyboard event, not " + Dom.eventString(event, "type"));
      }
      return new Keyboard(event);
    }

    @Override
    public ControlEvent control() {
      return new Control(event, this);
    }
  }

  static final class Basic extends NativeEvent implements BasicEvent {
    private Basic(JSObject event) {
      super(event);
    }
  }

  /// An event that reports which modifier keys were held, for [PointerEvent] and [KeyboardEvent].
  abstract static sealed class Modified extends NativeEvent permits Pointer, Keyboard {
    private Modified(JSObject event) {
      super(event);
    }

    public final boolean altKey() {
      return flag("altKey");
    }

    public final boolean ctrlKey() {
      return flag("ctrlKey");
    }

    public final boolean metaKey() {
      return flag("metaKey");
    }

    public final boolean shiftKey() {
      return flag("shiftKey");
    }
  }

  static final class Pointer extends Modified implements PointerEvent {
    private Pointer(JSObject event) {
      super(event);
    }

    @Override
    public int button() {
      return (int) Dom.number(event, "button");
    }

    @Override
    public double clientX() {
      return Dom.number(event, "clientX");
    }

    @Override
    public double clientY() {
      return Dom.number(event, "clientY");
    }

    @Override
    public int detail() {
      return (int) Dom.number(event, "detail");
    }
  }

  static final class Keyboard extends Modified implements KeyboardEvent {
    private Keyboard(JSObject event) {
      super(event);
    }

    @Override
    public String key() {
      return Dom.eventString(event, "key");
    }

    @Override
    public String code() {
      return Dom.eventString(event, "code");
    }

    @Override
    public boolean repeat() {
      return flag("repeat");
    }

    @Override
    public boolean composing() {
      return flag("isComposing");
    }
  }

  static final class Control extends NativeEvent implements ControlEvent {
    private final Source source;

    private Control(JSObject event, Source source) {
      super(event);
      this.source = source;
    }

    @Override
    public ControlState control() {
      var tag = source.tag();
      if (tag != HtmlTag.INPUT && tag != HtmlTag.TEXTAREA && tag != HtmlTag.SELECT) {
        throw new IllegalStateException(
            "Form state requires an input, textarea or select current target");
      }
      return ControlCodec.state(Dom.controlState(source.target(), event), source.composing());
    }
  }
}
