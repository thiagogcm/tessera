package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;

/// A view that can render page-level ids already used elsewhere on the page: by the second entry's
/// root (`shared-id`), by the host page (`page-note`) or by its own mount host. Each rejects the
/// render; the note's own id stays owned once committed.
final class CollisionsDemo {
  private final String host;
  private boolean claimShared;
  private boolean claimPage;
  private boolean claimHost;
  private boolean showNote = true;
  private int updates;

  private CollisionsDemo(String host) {
    this.host = host;
  }

  static BrowserRoot mount(String host) {
    return BrowserRoot.mount(host, new CollisionsDemo(host)::view);
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    return LiveElement.of(
            Elements.section()
                .ariaLabel("Page ids")
                .children(
                    Elements.h2().text("Page ids"),
                    Elements.p().role(Role.STATUS).text("Updates: " + updates)))
        .children(
            Ui.button("Update", root.update(() -> updates++)),
            Ui.button("Claim second entry id", root.update(() -> claimShared = true)),
            Ui.button("Claim page id", root.update(() -> claimPage = true)),
            Ui.button("Claim host id", root.update(() -> claimHost = true)),
            Ui.button(
                "Release ids",
                root.update(() -> {
                  claimShared = false;
                  claimPage = false;
                  claimHost = false;
                })),
            Ui.button("Toggle note", root.update(() -> showNote = !showNote)))
        .child(notes());
  }

  private IntoElement notes() {
    return Elements.fragment()
        .children(
            showNote
                ? Elements.p().id(ElementRef.page("collisions-note")).text("Owned note")
                : Elements.empty(),
            claimShared
                ? Elements.p().id(ElementRef.page("shared-id")).text("Claimed")
                : Elements.empty(),
            claimPage
                ? Elements.p().id(ElementRef.page("page-note")).text("Claimed")
                : Elements.empty(),
            claimHost ? Elements.p().id(ElementRef.page(host)).text("Claimed") : Elements.empty());
  }
}
