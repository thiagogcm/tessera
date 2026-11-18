package dev.nthings.tessera.wasm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;

/// State and native resources owned by one mounted widget host. The reconciler creates a controller
/// from the committed host and its binding, so a constructor installs listeners and reads the first
/// binding; [#update] follows at once and after every commit that retains the host.
abstract class WidgetController {
  final JSObject node;
  final BrowserRoot root;
  private final List<JSObject> listeners = new ArrayList<>();
  private boolean disposed;

  WidgetController(JSObject node, BrowserRoot root) {
    this.node = node;
    this.root = root;
  }

  /// Runs before each commit patches the DOM, while the previous references still hold.
  void beforePatch() {}

  /// Applies the latest binding. It runs on every commit, so it should return early when nothing it
  /// drives changed.
  abstract void update(WidgetBinding binding);

  final JSObject listen(JSObject target, String type, Consumer<JSObject> handler) {
    var listener =
        Dom.listen(
            target,
            type,
            event -> {
              if (!disposed && !root.closed()) {
                root.runScheduled(() -> handler.accept(event), false);
              }
            });
    listeners.add(listener);
    return listener;
  }

  final void unlisten(JSObject listener) {
    listeners.remove(listener);
    Dom.release(listener);
  }

  /// Runs an application callback, then requests a render even if it fails, so bound state
  /// reconciles with whatever the callback did.
  final void notifyApplication(Runnable callback) {
    try {
      callback.run();
    } finally {
      root.invalidate();
    }
  }

  /// The item whose element has focus, or `null`. Call it from [#beforePatch] to remember focus
  /// before the patch can move or replace the element.
  final <T> @Nullable T focusedItem(List<T> items, Function<T, JSObject> element) {
    if (items.isEmpty()) {
      return null;
    }
    var active = Dom.activeElement();
    if (!WidgetDom.contains(node, active)) {
      return null;
    }
    for (var item : items) {
      if (WidgetDom.same(element.apply(item), active)) {
        return item;
      }
    }
    return null;
  }

  /// The item [#focusedItem] remembered, when an update should return focus to its replacement: its
  /// element still has focus, or the patch left focus on the document.
  static <T> @Nullable T focusToRestore(@Nullable T before, Function<T, JSObject> element) {
    if (before == null) {
      return null;
    }
    var active = Dom.activeElement();
    return WidgetDom.same(element.apply(before), active) || WidgetDom.noFocus(active)
        ? before
        : null;
  }

  final void close() {
    disposed = true;
    listeners.forEach(Dom::release);
    listeners.clear();
    dispose();
  }

  void dispose() {}
}
