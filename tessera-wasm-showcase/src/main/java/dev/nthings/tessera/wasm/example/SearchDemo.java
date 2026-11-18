package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.Live;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import dev.nthings.tessera.wasm.LiveSearchPalette;
import dev.nthings.tessera.wasm.LiveTooltip;
import java.time.Duration;

/// A search palette whose results a background sync changes a minute after mounting, and a tooltip
/// on a trigger with a dotted page id that the same sync retires.
final class SearchDemo {
  private boolean synced;

  static BrowserRoot mount(String host) {
    return BrowserRoot.mount(host, new SearchDemo()::view);
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    var palette = SearchPalette.of().triggerLabel("Find pages");
    if (synced) {
      palette.item("Guides", "Café three", "#cafe-three");
    }
    palette.item("Guides", "Café one", "#cafe-one");
    if (!synced) {
      palette.item("Guides", "Café two", "#cafe-two");
    }
    palette.item("Reference", "API", "#api");
    var search = LiveSearchPalette.of(palette);
    var section =
        LiveElement.of(Elements.section().ariaLabel("Search"))
            .child(
                Live.effect(
                    "sync",
                    lifetime ->
                        root.host()
                            .within(lifetime)
                            .timeout(Duration.ofMinutes(1), () -> synced = true)))
            .child(search.triggerButton())
            .child(search);
    if (!synced) {
      section.child(
          LiveTooltip.of(
              Elements.button()
                  .id(ElementRef.page("search.tip"))
                  .interestTooltip(hint -> hint.text("More search details"))
                  .text("Search tip")));
    }
    return section;
  }
}
