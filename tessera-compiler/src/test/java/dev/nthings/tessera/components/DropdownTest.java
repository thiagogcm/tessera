package dev.nthings.tessera.components;

import static dev.nthings.tessera.document.Documents.deferred;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderDocumentMain;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.support.AccessibleText;
import dev.nthings.tessera.support.RenderedPage;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dropdown")
class DropdownTest {

  private static RenderedPage page;

  @BeforeAll
  static void renderDropdownWithModal() {
    var modal = Modal.of("Confirm Deployment").scopeId("confirm-deploy");
    var dropdown =
        Dropdown.of("Actions")
            .scopeId("actions-menu")
            .item(Dropdown.Item.link("/logs", "Scope Logs").shortcut("G L"))
            .item(Dropdown.Item.link("/status", "Status"))
            .separator()
            .item(
                Dropdown.Item.command(
                    "open-deploy", modal.dialogRef(), BuiltinCommand.SHOW_MODAL, "Deploy Now"));
    page = renderDocumentMain(DocumentContent.of(dropdown), deferred(modal, 10));
  }

  private static Element named(RenderedPage page, String selector, String name) {
    return page.elements(selector).stream()
        .filter(element -> AccessibleText.name(element).equals(name))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No " + selector + " named " + name));
  }

  @Test
  @DisplayName("the trigger toggles an auto popover holding the menu items")
  void triggerTogglesItsPanel() {
    var trigger = named(page, "button", "Actions");
    var panel = page.target(trigger, "commandfor");

    assertThat(trigger).hasAttribute("command", "toggle-popover");
    assertThat(panel).hasAttribute("popover", "auto");
    assertThat(panel.select("a"))
        .extracting(AccessibleText::of)
        .containsExactly("Scope Logs", "Status");
  }

  @Test
  @DisplayName("a command item opens a modal placed later on the page")
  void commandItemOpensModal() {
    var deploy = named(page, "button", "Deploy Now");

    assertThat(deploy).hasAttribute("command", "show-modal");
    var dialog = page.target(deploy, "commandfor");
    assertThat(dialog).hasTagName("dialog");
    assertThat(page.target(dialog, "aria-labelledby")).hasText("Confirm Deployment");
  }

  @Test
  @DisplayName("an invoker outside the menu can toggle its panel")
  void panelRefExposesPanel() {
    var reference = ElementRef.scoped("tools-panel");
    var outside =
        renderMain(
            Dropdown.of("Tools").panelRef(reference).item(Dropdown.Item.link("/logs", "Logs")),
            Button.of("Toggle tools").command(reference, BuiltinCommand.TOGGLE_POPOVER));

    assertThat(outside.target(named(outside, "button", "Toggle tools"), "commandfor"))
        .isSameAs(outside.target(named(outside, "button", "Tools"), "commandfor"));
  }

  @Test
  @DisplayName("a shortcut hint is shown but kept out of the item's name")
  void shortcutHintIsHidden() {
    var logs = named(page, "a", "Scope Logs");

    assertThat(logs.text()).contains("G L");
    assertThat(named(page, "a", "Status").select("[aria-hidden=true]")).isEmpty();
  }
}
