package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.css.CssLength;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CardGrid")
class CardGridTest {

  @Test
  @DisplayName("scopes each card's scope id under the grid's id, assigning one when absent")
  void scopesCardViewIdsUnderGridId() {
    var grid =
        CardGrid.of(
                CssLength.rem(18),
                Card.of("Multi-Phase Compiler").id("architecture"),
                Card.of("Reactive Tokens"))
            .id("feature-cards")
            .gap(CssLength.rem(1.25));

    assertThat(renderMain(grid).elements("[data-scope-id^=feature-cards--]"))
        .extracting(card -> card.attr("data-scope-id"))
        .containsExactly("feature-cards--architecture", "feature-cards--card-1");
  }
}
