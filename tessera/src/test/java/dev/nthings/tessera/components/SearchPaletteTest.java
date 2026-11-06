package dev.nthings.tessera.components;

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
          SearchPalette.create("site-search")
              .item("Guides", "Getting Started", "/getting-started", "Introduction", "New");
      page = renderMain(palette.triggerButton("open-search", "Search"), palette.asDeferred(50));
    }

    @Test
    @DisplayName("is opened as a modal by its trigger through an invoker command")
    void triggerOpensDialogAsModal() {
      var dialog = page.element("dialog[closedby=any]");

      assertThat(page.elementById("open-search"))
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
              SearchPalette.create("cmd-palette")
                  .item("Routes", "Telemetry", "/telemetry", "Real-time metrics")
                  .item("Routes", "Typography", "/typography", "Font hierarchy")
                  .item("Actions", "Toggle Dark Mode", "/theme", "Switch themes", "Action"));
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
  }
}
