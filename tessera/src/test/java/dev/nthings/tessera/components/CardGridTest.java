package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.css.CssLength;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CardGrid")
class CardGridTest {

  @Test
  @DisplayName("scopes each card's view id under the grid's id")
  void scopesCardViewIdsUnderGridId() {
    var grid =
        CardGrid.of(
            "feature-cards",
            CssLength.rem(18),
            CssLength.rem(1.25),
            Card.create("architecture").title("Multi-Phase Compiler"),
            Card.create("tokens").title("Reactive Tokens"));

    assertThat(renderMain(grid).elements("[data-view-id^=feature-cards--card-]"))
        .extracting(card -> card.attr("data-view-id"))
        .containsExactly("feature-cards--card-architecture", "feature-cards--card-tokens");
  }
}
