package dev.nthings.tessera.components;

import static dev.nthings.tessera.document.Documents.deferred;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderDocumentMain;

import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.support.AccessibleText;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SearchPalette")
class SearchPaletteTest {

  private static RenderedPage page;

  @BeforeAll
  static void renderPalette() {
    var palette =
        SearchPalette.of()
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

  @Test
  @DisplayName("the trigger opens a named dialog with a labelled search field and a close button")
  void triggerOpensDialog() {
    var trigger = page.element("main > button");
    assertThat(trigger).hasAttribute("command", "show-modal");
    assertThat(trigger.text()).startsWith("Search");

    var dialog = page.target(trigger, "commandfor");
    assertThat(dialog).hasTagName("dialog");
    assertThat(dialog.attr("aria-label")).isNotBlank();
    assertThat(dialog.selectFirst("input[type=search]").attr("aria-label")).isNotBlank();
    var close = dialog.selectFirst("button[command=close]");
    assertThat(close.attr("aria-label")).isNotBlank();
    assertThat(page.target(close, "commandfor")).isSameAs(dialog);
  }

  @Test
  @DisplayName("results read in group order, each group heading before its links")
  void groupsItemsByGroup() {
    var results = page.element("dialog [role=region]");

    assertThat(results.text())
        .containsSubsequence("Routes", "Telemetry", "Typography", "Actions", "Toggle Dark Mode");
    assertThat(results.select("a"))
        .extracting(link -> link.attr("href"))
        .containsExactly("/telemetry", "/typography", "/theme");
  }

  @Test
  @DisplayName("each result reads as its title, description and badge, without the arrow")
  void resultNamesIncludeTitleAndDescription() {
    var names = page.elements("dialog a").stream().map(AccessibleText::of).toList();

    assertThat(names).hasSize(3).noneMatch(name -> name.contains("→"));
    assertThat(names.get(0)).containsSubsequence("Telemetry", "Real-time metrics");
    assertThat(names.get(1)).isEqualTo("Typography");
    assertThat(names.get(2)).containsSubsequence("Toggle Dark Mode", "Switch themes", "Action");
  }
}
