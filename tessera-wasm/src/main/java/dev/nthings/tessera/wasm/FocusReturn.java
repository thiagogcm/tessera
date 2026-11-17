package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.ElementRef;
import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;

/// The element that had focus when [Host#rememberFocus()] was called, for restoring it later, for
/// example after a dialog closes. The element itself is never exposed.
///
/// A focus return is used once: restoring it releases the element, and it does nothing once its
/// host's lifetime or root is disposed.
public final class FocusReturn {
  private final Host host;
  private @Nullable JSObject element;
  private boolean consumed;

  FocusReturn(Host host, @Nullable JSObject element) {
    this.host = host;
    this.element = element;
  }

  /// Focuses the remembered element if it is still in the document, and returns whether it did.
  public boolean restore() {
    return restoreFocus(null);
  }

  /// Focuses the remembered element, or `fallback` when a render replaced or removed it.
  public void restore(ElementRef fallback) {
    restoreFocus(fallback);
  }

  private boolean restoreFocus(@Nullable ElementRef fallback) {
    if (consumed) {
      return false;
    }
    consumed = true;
    var target = element;
    element = null;
    if (!host.alive()) {
      return false;
    }
    if (target != null && Dom.flag(target, "isConnected")) {
      Dom.focus(target);
      return true;
    }
    if (fallback != null) {
      host.focus(fallback);
      return true;
    }
    return false;
  }
}
