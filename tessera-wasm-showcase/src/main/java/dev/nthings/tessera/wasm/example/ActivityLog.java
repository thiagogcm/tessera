package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// An activity panel in its own root, listing what the other views on the page reported. It
/// outlives those roots, so it still shows what happened while one of them closed.
final class ActivityLog {
  private final List<String> entries = new ArrayList<>();
  private @Nullable BrowserRoot root;

  BrowserRoot mount(String host) {
    var mounted = BrowserRoot.mount(host, this::view);
    root = mounted;
    return mounted;
  }

  /// Appends `entry` and refreshes the panel.
  void record(String entry) {
    entries.add(entry);
    var mounted = root;
    if (mounted != null) {
      mounted.invalidate();
    }
  }

  private LiveContent view(LiveContext context) {
    var list = Elements.ol().ariaLabel("Activity");
    entries.forEach(entry -> list.child(Elements.li().text(entry)));
    return LiveContent.of(Elements.section().children(Elements.h2().text("Activity"), list));
  }
}
