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
/// render.
final class CollisionsDemo {
  private final String host;
  private boolean claimShared;
  private boolean claimPage;
  private boolean claimHost;
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
                })))
        .child(notes());
  }

  private IntoElement notes() {
    return Elements.fragment()
        .children(
            claimShared
                ? Elements.p().id(ElementRef.page("shared-id")).text("Claimed")
                : Elements.empty(),
            claimPage
                ? Elements.p().id(ElementRef.page("page-note")).text("Claimed")
                : Elements.empty(),
            claimHost ? Elements.p().id(ElementRef.page(host)).text("Claimed") : Elements.empty());
  }
}
