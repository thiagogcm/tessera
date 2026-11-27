package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.form;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.document.Documents.deferred;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderDocumentMain;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.PopoverMode;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.support.AccessibleText;
import dev.nthings.tessera.support.RenderedPage;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("buttons")
class ButtonTest {

  private static Element button(RenderedPage page, String name) {
    return page.elements("button").stream()
        .filter(button -> AccessibleText.name(button).equals(name))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No button named " + name));
  }

  @Test
  @DisplayName("commands reach a dialog or popover placed later, even from a disabled button")
  void bindsCommandsToDeferredTargets() {
    var dialog = Modal.of("Confirm").scopeId("confirm");
    var hint = ElementRef.scoped("hint");
    var page =
        renderDocumentMain(
            DocumentContent.of(
                Button.of("Review").command(dialog.dialogRef(), BuiltinCommand.SHOW_MODAL)),
            DocumentContent.of(
                Button.of("Review later")
                    .command(dialog.dialogRef(), BuiltinCommand.SHOW_MODAL)
                    .disabled()),
            DocumentContent.of(Button.of("Peek").command(hint, BuiltinCommand.TOGGLE_POPOVER)),
            deferred(dialog, 1),
            deferred(div().id(hint).popover(PopoverMode.AUTO).text("Details"), 2));

    var review = button(page, "Review");
    var later = button(page, "Review later");
    assertThat(review).hasAttribute("command", "show-modal").doesNotHaveAttribute("disabled");
    assertThat(later).hasAttribute("command", "show-modal").hasAttribute("disabled");
    assertThat(page.target(review, "commandfor")).hasTagName("dialog");
    assertThat(page.target(later, "commandfor")).isSameAs(page.target(review, "commandfor"));
    var peek = button(page, "Peek");
    assertThat(peek).hasAttribute("command", "toggle-popover");
    assertThat(page.target(peek, "commandfor")).hasAttribute("popover").hasText("Details");
  }

  @Test
  @DisplayName("a button submits its form only when asked to")
  void submitsOnlyWhenAsked() {
    var page = renderMain(form().children(Button.of("Cancel"), Button.of("Save").submit()));

    assertThat(button(page, "Cancel")).hasAttribute("type", "button");
    assertThat(button(page, "Save")).hasAttribute("type", "submit");
  }

  @Test
  @DisplayName("an interest tooltip is a tooltip the button points at")
  void interestTooltipAnchorsAHint() {
    var page = renderMain(Button.of("Hover me").interestTooltip(tip -> tip.text("Saved")));

    assertThat(page.target(button(page, "Hover me"), "interestfor"))
        .hasAttribute("role", "tooltip")
        .hasText("Saved");
  }

  @Test
  @DisplayName("an icon button is named by its label and hides its glyph")
  void iconButtonKeepsName() {
    var page = renderMain(IconButton.of("Dismiss", span().text("×")));

    var dismiss = button(page, "Dismiss");
    assertThat(dismiss).hasAttribute("type", "button");
    assertThat(AccessibleText.of(dismiss)).isEmpty();
    assertThat(dismiss.select("[aria-hidden=true]").text()).isEqualTo("×");
  }

  private static Card toolbar(String id) {
    var panel = ElementRef.scoped("panel");
    return Card.of("Toolbar")
        .scopeId(id)
        .body(
            Button.of("Save")
                .id(ElementRef.scoped("save"))
                .command(panel, BuiltinCommand.TOGGLE_POPOVER),
            IconButton.of("Refresh", span().text("↻")).id(ElementRef.scoped("refresh")),
            div().id(panel).popover(PopoverMode.AUTO).text("Saved"));
  }

  @Test
  @DisplayName("buttons in two toolbars get distinct ids and target their own toolbar")
  void refsResolveInScope() {
    var page = renderMain(toolbar("editor"), toolbar("viewer"));

    assertThat(page.elements("[id]")).extracting(Element::id).doesNotHaveDuplicates();
    assertThat(page.elements("button[id]")).hasSize(4);
    for (var save : page.elements("button[commandfor]")) {
      assertThat(page.target(save, "commandfor").closest("article"))
          .isSameAs(save.closest("article"));
    }
  }

  @Test
  @DisplayName("busy feedback keeps each button's accessible name")
  void busyKeepsTheName() {
    var page =
        renderMain(
            Button.of("Save").busy().disabled(),
            Button.of("Publish draft", span().text("Publish")).busy(),
            IconButton.of("Refresh", span().text("↻")).busy().disabled());

    assertThat(page.elements("button[aria-busy=true]"))
        .extracting(button -> AccessibleText.name(button) + " " + button.hasAttr("disabled"))
        .containsExactly("Save true", "Publish draft false", "Refresh true");
  }
}
