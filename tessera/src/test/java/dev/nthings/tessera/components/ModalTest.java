package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.deferred;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.core.ElementRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Modal and Drawer")
class ModalTest {

  @Test
  @DisplayName("dialogs are light-dismiss and open or close via invoker commands")
  void lightDismissAndInvokerCommands() {
    var modal =
        Modal.of("Confirm Deployment")
            .id("confirm-deploy")
            .dialogRef(ElementRef.scoped("confirmation"))
            .description("Production rollout")
            .bodyText("Deploy to all 4 regions?");
    var drawer =
        Drawer.of("Live Inspector")
            .id("inspector")
            .side(Drawer.Side.INLINE_START)
            .dialogRef(ElementRef.scoped("details"))
            .description("Cascade state")
            .body(p().text("7 cascade layers active."));
    var preview = Modal.of("Preview").bodyText("Shown before the page is published.").opened();

    var page =
        renderMain(
            modal.triggerButton("Open Modal"),
            drawer.triggerButton("Open Drawer"),
            deferred(modal, 10),
            deferred(drawer, 20),
            preview);

    assertThat(page.elements("dialog[closedby=any]"))
        .extracting(e -> e.id())
        .containsExactlyInAnyOrder(
            "confirm-deploy--confirmation", "inspector--details", "modal-1--dialog");
    assertThat(page.element("button:contains(Open Modal)"))
        .hasAttribute("command", "show-modal")
        .hasAttribute("commandfor", "confirm-deploy--confirmation");
    assertThat(page.element("button:contains(Open Drawer)"))
        .hasAttribute("commandfor", "inspector--details");
    assertThat(page.element("dialog[data-scope^=drawer-start-] h2")).hasText("Live Inspector");
    assertThat(page.element("dialog[open]")).hasAttribute("open");
    assertThat(page.elementById("confirm-deploy--close"))
        .hasAttribute("command", "close")
        .hasAttribute("commandfor", "confirm-deploy--confirmation");
    assertThat(page.elementById("inspector--close"))
        .hasAttribute("commandfor", "inspector--details");
  }
}
