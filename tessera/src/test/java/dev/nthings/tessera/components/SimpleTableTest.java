package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.code;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SimpleTable")
class SimpleTableTest {

  @Test
  @DisplayName("renders a caption, column headers and text rows")
  void rendersCaptionHeadersAndRows() {
    var page =
        renderMain(
            SimpleTable.of()
                .caption("Cascade layers, lowest to highest priority")
                .headers("Layer", "Contents")
                .row("reset", "Box sizing and margins")
                .row("theme", "Design tokens as custom properties"));

    assertThat(page.element("main > table > caption"))
        .hasText("Cascade layers, lowest to highest priority");
    assertThat(page.elements("thead th[scope=col]"))
        .extracting(Element::text)
        .containsExactly("Layer", "Contents");
    assertThat(page.elements("tbody tr")).hasSize(2);
    assertThat(page.elements("tbody tr").getFirst().select("td"))
        .extracting(Element::text)
        .containsExactly("reset", "Box sizing and margins");
  }

  @Test
  @DisplayName("keeps links and code inside cells")
  void keepsMarkupInsideCells() {
    var page =
        renderMain(
            SimpleTable.of()
                .headers("Component", "Usage")
                .row(a("/components/card").text("Card"), code().text("Card.of(\"Title\")")));

    var cells = page.elements("tbody td");
    assertThat(cells.getFirst().selectFirst("a"))
        .hasAttribute("href", "/components/card")
        .hasText("Card");
    assertThat(cells.getLast().selectFirst("code")).hasText("Card.of(\"Title\")");
  }

  @Test
  @DisplayName("omits the header row when no headers are declared")
  void omitsHeaderRowWithoutHeaders() {
    var page = renderMain(SimpleTable.of().caption("Solo").row("Only cell"));

    assertThat(page.elements("thead")).isEmpty();
    assertThat(page.element("tbody td")).hasText("Only cell");
  }
}
