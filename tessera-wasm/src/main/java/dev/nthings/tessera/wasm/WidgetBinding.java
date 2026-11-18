package dev.nthings.tessera.wasm;

import org.graalvm.webimage.api.JSObject;

/// Widget configuration carried by the linked tree. Only a committed host can create a controller.
interface WidgetBinding {
  WidgetController create(JSObject node, BrowserRoot root);
}
