package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dropdown")
class DropdownTest {

  private static RenderedPage page;

  @BeforeAll
  static void renderDropdownWithModal() {
    var modal = Overlays.modal("confirm-deploy", "Confirm Deployment");
    var dropdown =
        Dropdown.create("actions-menu", "Actions")
            .linkItem("/logs", "View Logs", "G L")
            .separator()
            .commandItem("open-deploy", modal.dialogRef(), BuiltinCommand.SHOW_MODAL, "Deploy Now");
    page = renderMain(dropdown, modal.asDeferred(10));
  }

  @Test
  @DisplayName("its trigger toggles the adjacent auto popover")
  void triggerTogglesAdjacentAutoPopover() {
    var trigger = page.element("button.dropdown-trigger");
    var popover = trigger.nextElementSibling();

    assertThat(popover).hasAttribute("popover", "auto");
    assertThat(trigger)
        .hasAttribute("commandfor", popover.id())
        .hasAttribute("command", "toggle-popover");
  }

  @Test
  @DisplayName("exposes its panel to invokers outside the view")
  void exposesItsPanelToInvokersOutsideTheView() {
    var reference = ElementRef.scoped("tools-panel");
    var dropdown =
        Dropdown.create("tools", "Tools").panelRef(reference).linkItem("/logs", "View Logs", "G L");
    var outside =
        renderMain(
            dropdown, Button.of("Toggle tools").command(reference, BuiltinCommand.TOGGLE_POPOVER));
    var panel = outside.element("button.dropdown-trigger").nextElementSibling();

    assertThat(outside.element("main > button.button"))
        .hasAttribute("commandfor", panel.id())
        .hasAttribute("command", "toggle-popover");
  }

  @Test
  @DisplayName("claims no menu semantics it does not implement")
  void claimsNoFalseMenuSemantics() {
    assertThat(page.element("button.dropdown-trigger")).doesNotHaveAttribute("aria-haspopup");
  }

  @Test
  @DisplayName("a command item opens the modal declared elsewhere on the page")
  void commandItemOpensModalDeclaredElsewhere() {
    assertThat(page.elementById("actions-menu--open-deploy"))
        .hasAttribute("command", "show-modal")
        .hasAttribute("commandfor", "confirm-deploy--dialog");
  }
}
