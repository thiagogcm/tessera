package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.wasm.BrowserEntry;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import java.util.Map;

/// A second compiled entry point sharing the page with [WebImageDemo].
public final class SecondEntry {
  private int count;

  private SecondEntry() {}

  public static void main(String[] args) {
    var entry = new SecondEntry();
    BrowserEntry.start(args, Map.of("counter", host -> BrowserRoot.mount(host, entry::view)));
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    return LiveElement.of(
            Elements.section()
                .ariaLabel("Second entry")
                .children(
                    Elements.h2().text("Second entry"),
                    Elements.p()
                        .id(ElementRef.page("shared-id"))
                        .text("Owned by the second entry")))
        .child(Ui.button("Second increment", root.update(() -> count++)))
        .child(Elements.p().text("Second count: " + count));
  }
}
