package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;

/// Cross-root identity evidence: explicit ids already used by another entry's root, by the host
/// page, or by the mount host itself reject the candidate instead of producing ambiguous
/// references. The note's id is owned once committed; toggling the note off and on introduces it
/// again.
public final class CollisionsDemo {
  private final String host;
  private boolean claimShared;
  private boolean claimPage;
  private boolean claimHost;
  private boolean showNote = true;
  private int renders;

  private CollisionsDemo(String host) {
    this.host = host;
  }

  public static BrowserRoot mount(String host) {
    return BrowserRoot.mount(host, new CollisionsDemo(host)::view);
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    renders++;
    return LiveElement.of(
            Elements.section()
                .attr("data-collisions-renders", Integer.toString(renders))
                .child(Elements.h2().text("Collisions")))
        .children(
            Ui.button("Claim shared id", root.update(() -> claimShared = true)),
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
                ? Elements.p().id(ElementRef.page("collisions-note")).text("Owned")
                : Elements.empty(),
            // `shared-id` belongs to the second entry's root, `error` to the host page, and the
            // host id to this root's mount host element.
            claimShared
                ? Elements.p().id(ElementRef.page("shared-id")).text("Claimed")
                : Elements.empty(),
            claimPage
                ? Elements.p().id(ElementRef.page("error")).text("Claimed")
                : Elements.empty(),
            claimHost ? Elements.p().id(ElementRef.page(host)).text("Claimed") : Elements.empty());
  }
}
