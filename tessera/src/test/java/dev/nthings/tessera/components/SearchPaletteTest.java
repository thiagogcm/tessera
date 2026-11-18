package dev.nthings.tessera.components;

import static dev.nthings.tessera.document.Documents.deferred;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderDocumentMain;

import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SearchPalette")
class SearchPaletteTest {

  private static RenderedPage page;

  @BeforeAll
  static void renderPalette() {
    var palette =
        SearchPalette.of()
            .id("site-search")
            .triggerLabel("Search")
            .item(
                "Routes",
                SearchPalette.Item.of("Telemetry", "/telemetry").description("Real-time metrics"))
            .item("Routes", "Typography", "/typography")
            .item(
                "Actions",
                SearchPalette.Item.of("Toggle Dark Mode", "/theme")
                    .description("Switch themes")
                    .badge("Action"));
    page = renderDocumentMain(DocumentContent.of(palette.triggerButton()), deferred(palette, 50));
  }

  @Nested
  @DisplayName("dialog")
  class Dialog {

    @Test
    @DisplayName("the trigger opens the dialog with a show-modal command")
    void triggerShowsDialog() {
      var dialog = page.element("dialog[closedby=any]");

      assertThat(dialog).hasAttribute("id", "site-search--dialog");
      assertThat(page.element("main > button"))
          .hasText("Search AltK")
          .hasAttribute("commandfor", dialog.id())
          .hasAttribute("command", "show-modal")
          .hasAttribute("aria-keyshortcuts", "Alt+K")
          .hasAttribute("accesskey", "k");
      assertThat(dialog.selectFirst("input.palette-input"))
          .hasAttribute("placeholder", "Search documentation, routes, and sections...")
          .hasAttribute("aria-label", "Search query")
          .hasAttribute("autofocus");
    }

    @Test
    @DisplayName("the dialog is the customized tessera-search element")
    void dialogIsCustomized() {
      assertThat(page.element("dialog[closedby=any]")).hasAttribute("is", "tessera-search");
    }

    @Test
    @DisplayName("the close button targets the dialog with a close command")
    void closeButtonClosesDialog() {
      var dialog = page.element("dialog[closedby=any]");

      assertThat(dialog.selectFirst(".palette-close"))
          .hasAttribute("commandfor", dialog.id())
          .hasAttribute("command", "close")
          .hasAttribute("aria-label", "Close search palette")
          .hasText("Esc");
    }
  }

  @Nested
  @DisplayName("results")
  class Results {

    @Test
    @DisplayName("groups link items into one block per group")
    void groupsItemsByGroup() {
      var groups = page.elements("dialog .palette-group");

      assertThat(groups).hasSize(2);
      assertThat(groups)
          .extracting(group -> group.selectFirst(".palette-group-title").text())
          .containsExactly("Routes", "Actions");
      assertThat(groups.getFirst().select("a.palette-item"))
          .extracting(a -> a.attr("href"))
          .containsExactly("/telemetry", "/typography");
    }

    @Test
    @DisplayName("a description adds a secondary line and a badge replaces the arrow")
    void rendersDescriptionAndBadge() {
      var links = page.elements("a.palette-item");

      assertThat(links)
          .extracting(link -> link.selectFirst(".palette-item-title").text())
          .containsExactly("Telemetry", "Typography", "Toggle Dark Mode");
      assertThat(links.get(0).select(".palette-item-desc").text()).isEqualTo("Real-time metrics");
      assertThat(links.get(0).select(".palette-item-arrow")).hasSize(1);
      assertThat(links.get(1).select(".palette-item-desc")).isEmpty();
      assertThat(links.get(2).select(".palette-item-badge").text()).isEqualTo("Action");
      assertThat(links.get(2).select(".palette-item-arrow")).isEmpty();
    }

    @Test
    @DisplayName("results and the no-results status have scoped ids")
    void resultIds() {
      assertThat(page.elements("a.palette-item"))
          .extracting(link -> link.id())
          .containsExactly(
              "site-search--result-1", "site-search--result-2", "site-search--result-3");
      assertThat(page.element(".palette-empty")).hasAttribute("id", "site-search--empty");
    }
  }
}
