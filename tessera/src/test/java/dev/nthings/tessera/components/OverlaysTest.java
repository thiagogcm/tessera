package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Overlays")
class OverlaysTest {

  @Test
  @DisplayName("modals and drawers are light-dismiss dialogs")
  void modalsAndDrawersLightDismiss() {
    var modal =
        Overlays.modal("confirm-deploy", "Confirm Deployment")
            .description("Production rollout")
            .bodyText("Deploy to all 4 regions?");
    var drawer =
        Overlays.drawer("inspector", "Live Inspector")
            .subtitle("Cascade state")
            .body(p().text("7 cascade layers active."));
    var preview =
        Overlays.modal("preview", "Preview")
            .bodyText("Shown before the page is published.")
            .opened();

    var page =
        renderMain(
            modal.triggerButton("trigger-modal", "Open Modal"),
            drawer.triggerButton("trigger-drawer", "Open Drawer"),
            modal.asDeferred(10),
            drawer.asDeferred(20),
            preview);

    assertThat(page.elements("dialog[closedby=any]"))
        .extracting(e -> e.id())
        .containsExactlyInAnyOrder(
            "confirm-deploy--dialog", "inspector--dialog", "preview--dialog");
    assertThat(page.element("dialog[open]")).hasAttribute("open");
  }
}
