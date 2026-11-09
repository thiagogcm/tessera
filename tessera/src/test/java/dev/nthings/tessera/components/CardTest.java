package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.css.AspectRatio;
import dev.nthings.tessera.css.ObjectFit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Card")
class CardTest {

  @Test
  @DisplayName("a media card is <img>, <hgroup> with the stretched link, one <p> and a <footer>")
  void mediaCardKeepsFourRegionStructure() {
    var card =
        Card.of("Multi-Phase Compiler")
            .id("architecture")
            .media(
                Image.of("/assets/arch.webp", "Architecture diagram")
                    .aspectRatio(AspectRatio.of(16, 9))
                    .objectFit(ObjectFit.COVER))
            .description("Three-pass static pipeline")
            .bodyText("Separates layout expansion, validation, and paint.")
            .stretchedLink("/docs/compiler")
            .actions(button().text("Bookmark"));

    var media = renderMain(card).element("[data-view-id=architecture]");

    assertThat(media.children())
        .extracting(e -> e.tagName())
        .containsExactly("img", "hgroup", "p", "footer");
    assertThat(media.selectFirst("hgroup h3 a.card-link[href=/docs/compiler]")).isNotNull();
  }

  @Test
  @DisplayName("several body paragraphs are grouped into one element")
  void severalBodyParagraphsShareOneRegion() {
    var card =
        Card.of("Reactive Tokens")
            .bodyText("Uses native light-dark().")
            .bodyText("Zero runtime theme scripts.");

    var textOnly = renderMain(card).element("main > *");

    assertThat(textOnly.children()).extracting(e -> e.tagName()).containsExactly("h3", "div");
  }

  @Test
  @DisplayName("choice cards are <label>s wrapping their own radio or checkbox input")
  void choiceCardIsLabelWrappingItsInput() {
    var card = Card.of("Pro Tier").bodyText("Isolated workers.").radio("plan", "pro", true);

    var choice = renderMain(card).element("main > *");

    assertThat(choice).hasTagName("label");
    assertThat(choice.selectFirst("> input[type=radio][name=plan][checked]")).isNotNull();

    var checkbox =
        renderMain(Card.of("Alerts").checkbox("extras", "alerts", false)).element("label");
    assertThat(checkbox.selectFirst("> input[type=checkbox][name=extras]:not([checked])"))
        .isNotNull();
  }
}
