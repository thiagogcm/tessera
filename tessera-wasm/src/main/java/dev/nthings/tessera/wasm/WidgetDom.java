package dev.nthings.tessera.wasm;

import org.graalvm.webimage.api.JS;
import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;

/// Native operations used by the built-in widget controllers.
final class WidgetDom {
  private WidgetDom() {}

  @JS.Coerce
  @JS(
      args = {"node", "hidden"},
      value = "node.hidden = hidden;")
  static native void hidden(JSObject node, boolean hidden);

  /// Focuses `node` unless it already has focus.
  @JS(
      args = {"node"},
      value = "if (document.activeElement !== node) node.focus();")
  static native void focus(JSObject node);

  @JS.Coerce
  @JS(
      args = {"a", "b"},
      value = "return a === b;")
  static native boolean same(@Nullable JSObject a, @Nullable JSObject b);

  @JS.Coerce
  @JS(
      args = {"node", "other"},
      value = "return node.contains(other);")
  static native boolean contains(JSObject node, @Nullable JSObject other);

  @JS(
      args = {"event"},
      value = "return event.target;")
  static native @Nullable JSObject target(JSObject event);

  @JS.Coerce
  @JS(
      args = {"event", "selector"},
      value = "return event.target.closest?.(selector) || null;")
  static native @Nullable JSObject closest(JSObject event, String selector);

  @JS(
      args = {"event"},
      value = "return event.relatedTarget;")
  static native @Nullable JSObject related(JSObject event);

  @JS(
      args = {"event"},
      value = "return event.source || null;")
  static native @Nullable JSObject source(JSObject event);

  @JS.Coerce
  @JS(
      args = {"node"},
      value = "return getComputedStyle(node).direction === 'rtl';")
  static native boolean rtl(JSObject node);

  @JS(
      args = {"node"},
      value = "node.click();")
  static native void click(JSObject node);

  @JS.Coerce
  @JS(
      args = {"node"},
      value = "return node == null || node === document.body || node === document.documentElement;")
  static native boolean noFocus(@Nullable JSObject node);

  @JS("return document;")
  static native JSObject document();

  static boolean skipKey(JSObject event) {
    return Dom.flag(event, "defaultPrevented")
        || Dom.flag(event, "isComposing")
        || Dom.flag(event, "altKey")
        || Dom.flag(event, "ctrlKey")
        || Dom.flag(event, "metaKey");
  }

  @JS(
      args = {"node"},
      value = "node.requestClose();")
  static native void requestClose(JSObject node);
}
