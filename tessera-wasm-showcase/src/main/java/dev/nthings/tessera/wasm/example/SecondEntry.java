package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.wasm.BrowserEntry;
import dev.nthings.tessera.wasm.BrowserRoot;
import java.util.Map;

/// A second compiled entry point sharing the page with [WebImageDemo].
public final class SecondEntry {
  private int count;

  private SecondEntry() {}

  public static void main(String[] args) {
    var entry = new SecondEntry();
    BrowserEntry.start(args, Map.of("counter", host -> BrowserRoot.mount(host, entry::view)));
  }

  private IntoElement view(BrowserRoot root, DocumentContext context) {
    return Elements.section()
        .attr("data-second-count", Integer.toString(count))
        .children(
            Elements.h2().text("Second entry"),
            Elements.p().id(ElementRef.page("shared-id")).text("Owned by the second entry"),
            Button.of("Second increment").onClick(root.update(() -> count++)),
            Elements.p().text("Second count: " + count));
  }
}
