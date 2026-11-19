package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.PopoverMode;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.support.Diagnostics;
import java.util.stream.Stream;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Card")
class CardTest {

  @Test
  @DisplayName("a stretched link is the card's only link, named by its title, beside its actions")
  void stretchedLinkIsTheOnlyLink() {
    var card =
        Card.of("Multi-Phase Compiler")
            .media(Image.of("/assets/arch.webp", "Architecture diagram").dimensions(16, 9))
            .description("Three-pass static pipeline")
            .bodyText("Separates layout expansion, validation, and paint.")
            .stretchedLink("/docs/compiler")
            .actions(Button.of("Bookmark"));

    var rendered = renderMain(card).element("main > *");

    assertThat(rendered.select("a"))
        .singleElement()
        .satisfies(link ->
            assertThat(link)
                .hasAttribute("href", "/docs/compiler")
                .hasText("Multi-Phase Compiler"));
    assertThat(rendered.selectFirst("img")).hasAttribute("alt", "Architecture diagram");
    assertThat(rendered.selectFirst("button")).hasText("Bookmark");
    assertThat(rendered.selectFirst("button").closest("a")).isNull();
    assertThat(rendered.text())
        .contains(
            "Three-pass static pipeline", "Separates layout expansion, validation, and paint.");
  }

  static Stream<Arguments> choices() {
    return Stream.of(
        argumentSet("checked radio", Card.of("Pro").radio("plan", "pro").checked(), "radio", true),
        argumentSet(
            "unchecked checkbox",
            Card.of("Alerts").checkbox("extras", "alerts"),
            "checkbox",
            false),
        argumentSet(
            "checked before the choice",
            Card.of("Alerts").checked().checkbox("extras", "alerts"),
            "checkbox",
            true));
  }

  @ParameterizedTest
  @MethodSource("choices")
  @DisplayName("a choice card is a label around its own input, so the whole card toggles it")
  void choiceCardWrapsItsInput(Card card, String type, boolean checked) {
    var rendered = renderMain(card).element("main > *");

    assertThat(rendered).hasTagName("label");
    var input = rendered.selectFirst("input");
    assertThat(input).hasAttribute("type", type);
    assertThat(input.hasAttr("checked")).isEqualTo(checked);
  }

  @Test
  @DisplayName("radio cards sharing a name form one group with one choice checked")
  void radioCardsShareAGroup() {
    var page =
        renderMain(
            Card.of("Basic").radio("plan", "basic"),
            Card.of("Pro").radio("plan", "pro").checked(),
            Card.of("Team").radio("plan", "team"));

    var inputs = page.elements("input[type=radio]");
    assertThat(inputs)
        .extracting(input -> input.attr("name") + "=" + input.attr("value"))
        .containsExactly("plan=basic", "plan=pro", "plan=team");
    assertThat(page.elements("input[checked]"))
        .singleElement()
        .satisfies(input -> assertThat(input.closest("label").text()).isEqualTo("Pro"));
  }

  @Test
  @DisplayName("a card heading that skips a level is reported until the level is set")
  void headingLevelFollowsTheOutline() {
    assertThat(Diagnostics.of(() -> renderMain(h1().text("Plans"), Card.of("Pro"))))
        .singleElement()
        .satisfies(found -> {
          assertThat(found.rule()).isEqualTo("heading-level-skip");
          assertThat(found.components()).containsExactly("card[card-1]");
        });

    var page = renderMain(h1().text("Plans"), Card.of("Pro").headingLevel(2));
    assertThat(page.element("h2")).hasText("Pro");
  }

  private static Card plan(String title, String detail) {
    var details = ElementRef.scoped("details");
    return Card.of(title)
        .body(div().id(details).popover(PopoverMode.AUTO).text(detail))
        .actions(Button.of("Details").command(details, BuiltinCommand.TOGGLE_POPOVER));
  }

  private static CardGrid plans(String id) {
    return CardGrid.of(
            CssLength.rem(18),
            plan("Pro", "Isolated workers.").id("pro"),
            plan("Team", "Shared workers."))
        .id(id);
  }

  @Test
  @DisplayName("cards in two grids keep distinct ids and target their own content")
  void gridsIsolateCardReferences() {
    var page = renderMain(plans("monthly"), plans("yearly"));

    assertThat(page.elements("[id]")).extracting(Element::id).doesNotHaveDuplicates();
    var buttons = page.elements("button[commandfor]");
    assertThat(buttons).hasSize(4);
    for (var button : buttons) {
      assertThat(page.target(button, "commandfor").closest("article"))
          .isSameAs(button.closest("article"));
    }
  }
}
