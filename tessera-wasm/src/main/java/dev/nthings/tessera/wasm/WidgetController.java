package dev.nthings.tessera.wasm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.graalvm.webimage.api.JSObject;

/// State and native resources owned by one mounted widget host.
abstract class WidgetController {
  final JSObject node;
  final BrowserRoot root;
  private final List<JSObject> listeners = new ArrayList<>();
  private boolean disposed;

  WidgetController(JSObject node, BrowserRoot root) {
    this.node = node;
    this.root = root;
  }

  void beforePatch() {}

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

  final void close() {
    disposed = true;
    listeners.forEach(Dom::release);
    listeners.clear();
    dispose();
  }

  void dispose() {}
}
