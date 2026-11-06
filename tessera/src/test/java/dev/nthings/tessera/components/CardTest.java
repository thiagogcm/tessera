package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Card")
class CardTest {

  @Test
  @DisplayName("a media card is <img>, <hgroup> with the stretched link, one <p> and a <footer>")
  void mediaCardKeepsFourRegionStructure() {
    var card =
        Card.create("architecture")
            .mediaImg("/assets/arch.webp", "Architecture diagram", "16 / 9")
            .title("Multi-Phase Compiler")
            .subtitle("Three-pass static pipeline")
            .bodyText("Separates layout expansion, validation, and paint.")
            .stretchedLink("/docs/compiler")
            .action(button().text("Bookmark"));

    var media = renderMain(card).element("[data-view-id=card-architecture]");

    assertThat(media.children())
        .extracting(e -> e.tagName())
        .containsExactly("img", "hgroup", "p", "footer");
    assertThat(media.selectFirst("hgroup h3 a.card-link[href=/docs/compiler]")).isNotNull();
  }

  @Test
  @DisplayName("several body paragraphs are grouped into one element")
  void severalBodyParagraphsShareOneRegion() {
    var card =
        Card.create("tokens")
            .title("Reactive Tokens")
            .bodyText("Uses native light-dark().")
            .bodyText("Zero runtime theme scripts.");

    var textOnly = renderMain(card).element("[data-view-id=card-tokens]");

    assertThat(textOnly.children()).extracting(e -> e.tagName()).containsExactly("h3", "div");
  }

  @Test
  @DisplayName("a choice card is a <label> wrapping its own radio input")
  void choiceCardIsLabelWrappingItsInput() {
    var card =
        Card.create("tier-pro")
            .title("Pro Tier")
            .bodyText("Isolated workers.")
            .choiceRadio("plan", "pro", true);

    var choice = renderMain(card).element("[data-view-id=card-tier-pro]");

    assertThat(choice).hasTagName("label");
    assertThat(choice.selectFirst("> input[type=radio][name=plan][checked]")).isNotNull();
  }
}
