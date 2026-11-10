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
                    .element(
                        img -> img.aspectRatio(AspectRatio.of(16, 9)).objectFit(ObjectFit.COVER)))
            .description("Three-pass static pipeline")
            .bodyText("Separates layout expansion, validation, and paint.")
            .stretchedLink("/docs/compiler")
            .actions(button().text("Bookmark"));

    var media = renderMain(card).element("article[data-scope-id=architecture]");

    assertThat(media.children())
        .extracting(e -> e.tagName())
        .containsExactly("img", "hgroup", "p", "footer");
    assertThat(media.selectFirst("> img"))
        .hasAttribute("src", "/assets/arch.webp")
        .hasAttribute("alt", "Architecture diagram");
    assertThat(media.selectFirst("hgroup > p")).hasText("Three-pass static pipeline");
    assertThat(media.selectFirst("hgroup h3 a"))
        .hasClasses("card-link")
        .hasAttribute("href", "/docs/compiler")
        .hasText("Multi-Phase Compiler");
    assertThat(media.selectFirst("> p"))
        .hasText("Separates layout expansion, validation, and paint.");
    assertThat(media.selectFirst("footer button")).hasText("Bookmark");
  }

  @Test
  @DisplayName("several body paragraphs are grouped into one element")
  void bodyParagraphsShareOneRegion() {
    var card =
        Card.of("Reactive Tokens")
            .bodyText("Uses native light-dark().")
            .bodyText("Zero runtime theme scripts.");

    var textOnly = renderMain(card).element("main > article");

    assertThat(textOnly.children()).extracting(e -> e.tagName()).containsExactly("h3", "div");
    assertThat(textOnly.select("> div > p"))
        .extracting(e -> e.text())
        .containsExactly("Uses native light-dark().", "Zero runtime theme scripts.");
  }

  @Test
  @DisplayName("choice cards are <label>s wrapping their own radio or checkbox input")
  void choiceCardWrapsItsInput() {
    var card = Card.of("Pro Tier").bodyText("Isolated workers.").radio("plan", "pro").checked();

    var choice = renderMain(card).element("main > *");

    assertThat(choice).hasTagName("label");
    assertThat(choice.selectFirst("> input"))
        .hasAttribute("type", "radio")
        .hasAttribute("name", "plan")
        .hasAttribute("value", "pro")
        .hasAttribute("checked");

    var checkbox = renderMain(Card.of("Alerts").checkbox("extras", "alerts")).element("label");
    assertThat(checkbox.selectFirst("> input"))
        .hasAttribute("type", "checkbox")
        .hasAttribute("name", "extras")
        .hasAttribute("value", "alerts")
        .doesNotHaveAttribute("checked");
  }

  @Test
  @DisplayName("checked() before the choice still checks the input")
  void checkedBeforeChoiceApplies() {
    var card = Card.of("Alerts").checked().checkbox("extras", "alerts");

    assertThat(renderMain(card).element("label > input"))
        .hasAttribute("type", "checkbox")
        .hasAttribute("name", "extras")
        .hasAttribute("value", "alerts")
        .hasAttribute("checked");
  }
}
