package dev.nthings.tessera.wasm.example;

import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.wasm.BrowserRoot;

/// Cross-root identity evidence: explicit ids already used by another entry's root or by the
/// host page reject the candidate instead of producing ambiguous references. The note's id is
/// owned once committed; toggling the note off and on introduces it again.
public final class CollisionsDemo {
  private boolean claimShared;
  private boolean claimPage;
  private boolean showNote = true;
  private int renders;

  private CollisionsDemo() {}

  public static BrowserRoot mount(String host) {
    return BrowserRoot.mount(host, new CollisionsDemo()::view);
  }

  private IntoElement view(BrowserRoot root, DocumentContext context) {
    renders++;
    return Elements.section()
        .attr("data-collisions-renders", Integer.toString(renders))
        .children(
            Elements.h2().text("Collisions"),
            Button.of("Claim shared id").onClick(root.update(() -> claimShared = true)),
            Button.of("Claim page id").onClick(root.update(() -> claimPage = true)),
            Button.of("Release ids")
                .onClick(
                    root.update(
                        () -> {
                          claimShared = false;
                          claimPage = false;
                        })),
            Button.of("Toggle note").onClick(root.update(() -> showNote = !showNote)),
            showNote
                ? Elements.p().id(ElementRef.page("collisions-note")).text("Owned")
                : Elements.empty(),
            // `shared-id` belongs to the second entry's root, `error` to the host page.
            claimShared
                ? Elements.p().id(ElementRef.page("shared-id")).text("Claimed")
                : Elements.empty(),
            claimPage
                ? Elements.p().id(ElementRef.page("error")).text("Claimed")
                : Elements.empty());
  }
}
