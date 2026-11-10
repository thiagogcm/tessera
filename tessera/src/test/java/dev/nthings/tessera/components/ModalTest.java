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
  @DisplayName("dialogs are labelled, light-dismiss, and open or close via invoker commands")
  void labelledLightDismissAndInvokerCommands() {
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
    assertThat(page.elements("dialog[open]"))
        .extracting(e -> e.id())
        .containsExactly("modal-1--dialog");

    var modalDialog = page.elementById("confirm-deploy--confirmation");
    assertThat(modalDialog)
        .hasAttribute("aria-labelledby", "confirm-deploy--title")
        .hasAttribute("closedby", "any");
    assertThat(page.elementById(modalDialog.attr("aria-labelledby")))
        .hasTagName("h2")
        .hasText("Confirm Deployment");
    assertThat(modalDialog.select("p"))
        .extracting(e -> e.text())
        .containsExactly("Production rollout", "Deploy to all 4 regions?");

    var drawerDialog = page.elementById("inspector--details");
    assertThat(drawerDialog)
        .hasAttribute("aria-labelledby", "inspector--title")
        .hasAttribute("closedby", "any");
    assertThat(page.elementById(drawerDialog.attr("aria-labelledby")))
        .hasTagName("h2")
        .hasText("Live Inspector");
    assertThat(drawerDialog.select("p"))
        .extracting(e -> e.text())
        .containsExactly("Cascade state", "7 cascade layers active.");
    assertThat(drawerDialog.attr("data-scope")).startsWith("drawer-start-");
    assertThat(page.scopeBody(drawerDialog))
        .contains("margin-inline-start:0px")
        .contains("margin-inline-end:auto");

    var previewDialog = page.elementById("modal-1--dialog");
    assertThat(previewDialog).hasAttribute("open");
    assertThat(previewDialog.selectFirst("h2")).hasText("Preview");
    assertThat(previewDialog.selectFirst("p")).hasText("Shown before the page is published.");

    assertThat(page.element("button:contains(Open Modal)"))
        .hasAttribute("command", "show-modal")
        .hasAttribute("commandfor", modalDialog.id());
    assertThat(page.element("button:contains(Open Drawer)"))
        .hasAttribute("command", "show-modal")
        .hasAttribute("commandfor", drawerDialog.id());
    assertThat(page.elementById("confirm-deploy--close"))
        .hasAttribute("command", "close")
        .hasAttribute("commandfor", modalDialog.id())
        .hasText("Close");
    assertThat(page.elementById("inspector--close"))
        .hasAttribute("command", "close")
        .hasAttribute("commandfor", drawerDialog.id())
        .hasText("Close");
  }
}
