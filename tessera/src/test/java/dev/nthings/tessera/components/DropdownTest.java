package dev.nthings.tessera.components;

import static dev.nthings.tessera.document.Documents.deferred;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderDocumentMain;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dropdown")
class DropdownTest {

  private static RenderedPage page;

  @BeforeAll
  static void renderDropdownWithModal() {
    var modal = Modal.of("Confirm Deployment").id("confirm-deploy");
    var dropdown =
        Dropdown.of("Actions")
            .id("actions-menu")
            .item(Dropdown.Item.link("/logs", "Scope Logs").shortcut("G L"))
            .item(Dropdown.Item.link("/status", "Status"))
            .separator()
            .item(
                Dropdown.Item.command(
                    "open-deploy", modal.dialogRef(), BuiltinCommand.SHOW_MODAL, "Deploy Now"));
    page = renderDocumentMain(DocumentContent.of(dropdown), deferred(modal, 10));
  }

  @Test
  @DisplayName("its trigger toggles the adjacent auto popover")
  void triggerTogglesAdjacentAutoPopover() {
    var trigger = page.element("button.dropdown-trigger");
    var popover = trigger.nextElementSibling();

    assertThat(popover).hasAttribute("id", "actions-menu--panel").hasAttribute("popover", "auto");
    assertThat(trigger)
        .hasAttribute("id", "actions-menu--trigger")
        .hasAttribute("commandfor", "actions-menu--panel")
        .hasAttribute("command", "toggle-popover");
  }

  @Test
  @DisplayName("exposes its panel to an invoker outside the menu")
  void panelRefExposesPanel() {
    var reference = ElementRef.scoped("tools-panel");
    var dropdown =
        Dropdown.of("Tools")
            .id("tools")
            .panelRef(reference)
            .item(Dropdown.Item.link("/logs", "Scope Logs"));
    var outside =
        renderMain(
            dropdown, Button.of("Toggle tools").command(reference, BuiltinCommand.TOGGLE_POPOVER));
    var panel = outside.element("button.dropdown-trigger").nextElementSibling();

    assertThat(panel).hasAttribute("id", "tools--tools-panel").hasAttribute("popover", "auto");
    assertThat(outside.element("main > button.button"))
        .hasAttribute("commandfor", "tools--tools-panel")
        .hasAttribute("command", "toggle-popover");
  }

  @Test
  @DisplayName("the trigger sets no aria-haspopup")
  void triggerOmitsHaspopup() {
    assertThat(page.element("button.dropdown-trigger")).doesNotHaveAttribute("aria-haspopup");
  }

  @Test
  @DisplayName("a command item targets a modal declared elsewhere")
  void commandItemTargetsModal() {
    assertThat(page.elementById("actions-menu--open-deploy"))
        .hasAttribute("command", "show-modal")
        .hasAttribute("commandfor", "confirm-deploy--dialog");
  }

  @Test
  @DisplayName("a shortcut hint is hidden from assistive technology and omitted when absent")
  void shortcutHintIsHidden() {
    assertThat(page.element("a.dropdown-item[href=/logs] > .dropdown-hint"))
        .hasText("G L")
        .hasAttribute("aria-hidden", "true");
    assertThat(page.element("a.dropdown-item[href=/status]").select(".dropdown-hint")).isEmpty();
  }
}
