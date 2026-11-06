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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("buttons")
class ButtonTest {

  @Test
  @DisplayName("commands open a deferred dialog or popover, and a disabled button cannot")
  void commandsOpenDeferredTargets() {
    var dialog = Overlays.modal("confirm", "Confirm");
    var hint = ElementRef.named("hint");
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
            dialog.asDeferred(1),
            deferred(div().id(hint).popover(PopoverMode.AUTO).text("Details"), 2));

    var dialogEl = page.element("dialog");
    var popover = page.element("[popover]");
    assertThat(page.element("button.button--solid"))
        .hasAttribute("type", "button")
        .hasAttribute("command", "show-modal")
        .hasAttribute("commandfor", dialogEl.id())
        .doesNotHaveAttribute("disabled");
    assertThat(page.element("button[disabled]"))
        .hasAttribute("disabled")
        .hasAttribute("command", "show-modal")
        .hasAttribute("commandfor", dialogEl.id())
        .hasText("Review later");
    assertThat(page.element("button.button--ghost"))
        .hasAttribute("command", "toggle-popover")
        .hasAttribute("commandfor", popover.id());
    assertThat(popover).hasText("Details");
  }

  @Test
  @DisplayName("submission follows the submit choice rather than the surface")
  void submissionIsIndependentOfSurface() {
    var page =
        renderMain(
            form()
                .children(
                    Button.of("Cancel").variant(Button.Variant.SOLID),
                    Button.of("Save")
                        .submit()
                        .tone(Button.Tone.DANGER)
                        .variant(Button.Variant.OUTLINE)));

    assertThat(page.element("form button.button--solid"))
        .hasAttribute("type", "button")
        .hasText("Cancel");
    assertThat(page.element("form button.button--outline"))
        .hasAttribute("type", "submit")
        .hasClasses("button--danger")
        .hasText("Save");
  }

  @Test
  @DisplayName("a button can show an interest tooltip")
  void interestTooltipAnchorsAHint() {
    var page = renderMain(Button.of("Hover me").interestTooltip(tip -> tip.text("Saved")));

    var hint = page.element("[popover=hint]");
    assertThat(page.element("button")).hasAttribute("interestfor", hint.id()).hasText("Hover me");
    assertThat(hint).hasAttribute("role", "tooltip").hasText("Saved");
  }

  @Test
  @DisplayName("an icon button keeps its name, hides the icon, and shows keyboard focus")
  void iconButtonKeepsItsNameAndFocus() {
    var page = renderMain(IconButton.of("Dismiss", span().text("Dismiss")));

    var button = page.element("button");
    assertThat(button)
        .hasTagName("button")
        .hasAttribute("type", "button")
        .hasAttribute("aria-label", "Dismiss");
    assertThat(button.selectFirst(".button-icon"))
        .hasAttribute("aria-hidden", "true")
        .hasText("Dismiss");

    var scope = page.scopeBody(button);
    var media = scope.indexOf("@media (prefers-reduced-motion: no-preference)");
    assertThat(media).isPositive();
    assertThat(scope.substring(0, media))
        .contains(":scope:focus-visible{")
        .contains("outline:2px solid var(--accent-primary)")
        .doesNotContain("transition-duration:");
    assertThat(scope.substring(media))
        .contains("transition-duration:var(--control-feedback-duration)");
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
