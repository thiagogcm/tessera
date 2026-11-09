package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.deferred;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SearchPalette")
class SearchPaletteTest {

  @Nested
  @DisplayName("dialog")
  class Dialog {

    private static RenderedPage page;

    @BeforeAll
    static void renderPalette() {
      var palette =
          SearchPalette.of()
              .id("site-search")
              .triggerLabel("Search")
              .item(
                  "Guides",
                  SearchPalette.Item.of("Getting Started", "/getting-started")
                      .description("Introduction")
                      .badge("New"));
      page = renderMain(palette.triggerButton(), deferred(palette, 50));
    }

    @Test
    @DisplayName("is opened as a modal by its trigger through an invoker command")
    void triggerOpensDialogAsModal() {
      var dialog = page.element("dialog[closedby=any]");

      assertThat(dialog).hasAttribute("id", "site-search--dialog");
      assertThat(page.element("main > button"))
          .hasText("Search AltK")
          .hasAttribute("commandfor", dialog.id())
          .hasAttribute("command", "show-modal");
    }

    @Test
    @DisplayName("is closed by its close button through an invoker command")
    void closeButtonClosesDialog() {
      var dialog = page.element("dialog[closedby=any]");

      assertThat(dialog.selectFirst(".palette-close"))
          .hasAttribute("commandfor", dialog.id())
          .hasAttribute("command", "close");
    }
  }

  @Nested
  @DisplayName("results")
  class Results {

    private static RenderedPage page;

    @BeforeAll
    static void renderPalette() {
      page =
          renderMain(
              SearchPalette.of()
                  .item(
                      "Routes",
                      SearchPalette.Item.of("Telemetry", "/telemetry")
                          .description("Real-time metrics"))
                  .item("Routes", "Typography", "/typography")
                  .item(
                      "Actions",
                      SearchPalette.Item.of("Toggle Dark Mode", "/theme")
                          .description("Switch themes")
                          .badge("Action")));
    }

    @Test
    @DisplayName("link items are grouped by their group")
    void linkItemsAreGroupedByGroup() {
      var groups = page.elements("dialog .palette-group");

      assertThat(groups).hasSize(2);
      assertThat(groups.getFirst().select("a.palette-item"))
          .extracting(a -> a.attr("href"))
          .containsExactly("/telemetry", "/typography");
    }

    @Test
    @DisplayName("a description adds a secondary line and a badge replaces the arrow")
    void descriptionAndBadgeAreOptional() {
      var links = page.elements("a.palette-item");

      assertThat(links.get(0).select(".palette-item-desc").text()).isEqualTo("Real-time metrics");
      assertThat(links.get(0).select(".palette-item-arrow")).hasSize(1);
      assertThat(links.get(1).select(".palette-item-desc")).isEmpty();
      assertThat(links.get(2).select(".palette-item-badge").text()).isEqualTo("Action");
      assertThat(links.get(2).select(".palette-item-arrow")).isEmpty();
    }
  }
}
