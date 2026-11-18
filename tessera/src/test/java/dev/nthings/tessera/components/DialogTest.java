package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.document.Documents.deferred;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderDocumentMain;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.document.DocumentContent;
import java.util.stream.Stream;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Modal and Drawer")
class DialogTest {

  static Stream<Arguments> dialogs() {
    var modal =
        Modal.of("Confirm Deployment")
            .id("confirm-deploy")
            .description("Production rollout")
            .bodyText("Deploy to all 4 regions?");
    var drawer =
        Drawer.of("Confirm Deployment")
            .id("confirm-deploy")
            .side(Drawer.Side.INLINE_START)
            .description("Production rollout")
            .body(p().text("Deploy to all 4 regions?"));
    return Stream.of(
        argumentSet("modal", modal, modal.triggerButton("Deploy")),
        argumentSet("drawer", drawer, drawer.triggerButton("Deploy")));
  }

  @ParameterizedTest
  @MethodSource("dialogs")
  @DisplayName("its trigger opens a dialog named by its title, which its close button closes")
  void triggerAndCloseTargetTheDialog(IntoElement dialog, Button trigger) {
    var page = renderDocumentMain(DocumentContent.of(trigger), deferred(dialog, 10));

    var open = page.element("main button");
    assertThat(open).hasText("Deploy").hasAttribute("command", "show-modal");
    var shown = page.target(open, "commandfor");
    assertThat(shown).hasTagName("dialog").doesNotHaveAttribute("open");
    assertThat(page.target(shown, "aria-labelledby")).hasText("Confirm Deployment");
    assertThat(shown.select("p"))
        .extracting(Element::text)
        .containsExactly("Production rollout", "Deploy to all 4 regions?");
    var close = shown.selectFirst("button[command=close]");
    assertThat(close).hasText("Close");
    assertThat(page.target(close, "commandfor")).isSameAs(shown);
  }

  static Stream<Arguments> openedDialogs() {
    return Stream.of(
        argumentSet("modal", Modal.of("Preview").bodyText("Shown on load.").opened()),
        argumentSet("drawer", Drawer.of("Preview").body(p().text("Shown on load.")).opened()));
  }

  @ParameterizedTest
  @MethodSource("openedDialogs")
  @DisplayName("an opened dialog is open on load")
  void openedDialogIsOpen(IntoElement dialog) {
    var shown = renderMain(dialog).element("dialog");

    assertThat(shown).hasAttribute("open");
    assertThat(shown.text()).contains("Preview", "Shown on load.");
  }
}
