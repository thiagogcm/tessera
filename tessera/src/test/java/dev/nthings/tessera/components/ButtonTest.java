package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.deferred;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.form;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.PopoverMode;
import dev.nthings.tessera.css.Tone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("buttons")
class ButtonTest {

  @Test
  @DisplayName("commands bind a deferred dialog or popover target, even when disabled")
  void bindsCommandsToDeferredTargets() {
    var dialog = Modal.of("Confirm").id("confirm");
    var hint = ElementRef.scoped("hint");
    var page =
        renderMain(
            Button.of("Review").command(dialog.dialogRef(), BuiltinCommand.SHOW_MODAL),
            Button.of("Review later")
                .command(dialog.dialogRef(), BuiltinCommand.SHOW_MODAL)
                .variant(Button.Variant.OUTLINE)
                .disabled(),
            Button.of("Peek")
                .command(hint, BuiltinCommand.TOGGLE_POPOVER)
                .variant(Button.Variant.GHOST),
            deferred(dialog, 1),
            deferred(div().id(hint).popover(PopoverMode.AUTO).text("Details"), 2));

    assertThat(page.element("button.button--solid"))
        .hasAttribute("type", "button")
        .hasAttribute("command", "show-modal")
        .hasAttribute("commandfor", "confirm--dialog")
        .doesNotHaveAttribute("disabled");
    assertThat(page.element("button[disabled]"))
        .hasAttribute("disabled")
        .hasAttribute("command", "show-modal")
        .hasAttribute("commandfor", "confirm--dialog");
    assertThat(page.element("button.button--ghost"))
        .hasAttribute("command", "toggle-popover")
        .hasAttribute("commandfor", "hint");
    assertThat(page.element("[popover]")).hasText("Details");
  }

  @Test
  @DisplayName("submission follows the submit choice rather than the surface")
  void submissionIsIndependentOfSurface() {
    var page =
        renderMain(
            form()
                .children(
                    Button.of("Cancel").variant(Button.Variant.SOLID),
                    Button.of("Save").submit().tone(Tone.DANGER).variant(Button.Variant.OUTLINE)));

    assertThat(page.element("form button.button--solid"))
        .hasAttribute("type", "button")
        .hasText("Cancel");
    assertThat(page.element("form button.button--outline"))
        .hasAttribute("type", "submit")
        .hasClasses("button--danger")
        .hasText("Save");
  }

  @Test
  @DisplayName("interest tooltip anchors a labelled hint popover")
  void interestTooltipAnchorsAHint() {
    var page = renderMain(Button.of("Hover me").interestTooltip(tip -> tip.text("Saved")));

    var hint = page.element("[popover=hint]");
    assertThat(page.element("button")).hasAttribute("interestfor", hint.id()).hasText("Hover me");
    assertThat(hint).hasAttribute("role", "tooltip").hasText("Saved");
  }

  @Test
  @DisplayName("an icon button keeps its name, hides the icon, and shows keyboard focus")
  void iconButtonKeepsName() {
    var page = renderMain(IconButton.of("Dismiss", span().text("Dismiss")));

    var button = page.element("button");
    assertThat(button)
        .hasTagName("button")
        .hasAttribute("type", "button")
        .hasAttribute("aria-label", "Dismiss");
    assertThat(button.selectFirst(".button-icon"))
        .hasAttribute("aria-hidden", "true")
        .hasText("Dismiss");

    assertThat(page.ruleOf(button, "u-")).doesNotContain("transition-duration:");
    var interactive = page.ruleOf(button, "i-");
    var media = interactive.indexOf("@media (prefers-reduced-motion: no-preference)");
    assertThat(media).isPositive();
    assertThat(interactive.substring(0, media))
        .contains("&:focus-visible{outline:2px solid var(--accent-primary)")
        .doesNotContain("transition-duration:");
    assertThat(interactive.substring(media))
        .contains("transition-duration:var(--control-feedback-duration)");
  }

  @Test
  @DisplayName("a button binds and targets refs in the enclosing id scope")
  void refsResolveInScope() {
    var save = ElementRef.scoped("save");
    var panel = ElementRef.scoped("panel");
    var page =
        renderMain(
            Card.of("Toolbar")
                .id("toolbar")
                .body(
                    Button.of("Save").id(save).command(panel, BuiltinCommand.TOGGLE_POPOVER),
                    IconButton.of("Refresh", span().text("↻")).id(ElementRef.scoped("refresh")),
                    div().id(panel).popover(PopoverMode.AUTO).text("Saved")));

    assertThat(page.element("button:contains(Save)"))
        .hasAttribute("id", "toolbar--save")
        .hasAttribute("commandfor", "toolbar--panel")
        .doesNotHaveAttribute("data-scope");
    assertThat(page.element("button.button--icon")).hasAttribute("id", "toolbar--refresh");
    assertThat(page.css()).doesNotContain("data-scope=\"button");
  }

  @Test
  @DisplayName("busy feedback keeps the accessible name when the button is also disabled")
  void busyKeepsTheName() {
    var page =
        renderMain(
            Button.of("Save").busy().disabled(),
            Button.of("Publish draft", span().text("Publish")).busy(),
            IconButton.of("Refresh", span().text("↻")).busy().disabled());

    assertThat(page.element("button.button--solid"))
        .hasAttribute("aria-busy", "true")
        .hasAttribute("disabled")
        .hasText("Save")
        .doesNotHaveAttribute("aria-label");
    assertThat(page.element("button[aria-label=\"Publish draft\"]"))
        .hasAttribute("aria-busy", "true")
        .hasAttribute("aria-label", "Publish draft")
        .hasText("Publish")
        .doesNotHaveAttribute("disabled");
    assertThat(page.element("button.button--icon"))
        .hasAttribute("aria-busy", "true")
        .hasAttribute("disabled")
        .hasAttribute("aria-label", "Refresh");
  }
}
