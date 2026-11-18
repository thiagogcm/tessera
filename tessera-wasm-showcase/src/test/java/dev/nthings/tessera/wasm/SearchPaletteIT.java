package dev.nthings.tessera.wasm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;

/// The live search palette on the search page.
@DisplayName("Search palette")
class SearchPaletteIT {

  @BothEngines
  @DisplayName(
      "the palette filters, wraps keyboard focus, keeps it across result updates and opens a result")
  void paletteFindsAndOpensResults(Session session) {
    var page = session.page();
    page.clock().install();
    session.open("/search/");
    var trigger =
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Find pages"));
    trigger.click();
    var query = session.field("Search query");
    assertThat(query).isFocused();
    var palette = page.getByRole(AriaRole.DIALOG);
    var results = palette.getByRole(AriaRole.LINK);

    query.fill("missing");
    assertThat(results).hasCount(0);
    assertThat(palette.getByRole(AriaRole.STATUS)).isVisible();
    query.fill("guides cafe");
    assertThat(results).hasCount(2);
    query.press("ArrowUp");
    assertThat(results.last()).isFocused();
    assertThat(results.last()).containsText("Café two");
    page.keyboard().press("ArrowDown");
    assertThat(results.first()).isFocused();
    assertThat(results.first()).containsText("Café one");

    // The sync adds a result above the focused one and removes another.
    page.clock().runFor(60_000);
    assertThat(results).hasCount(2);
    assertThat(results.first()).containsText("Café three");
    assertThat(results.last()).containsText("Café one");
    assertThat(results.last()).isFocused();
    page.keyboard().press("ArrowDown");
    assertThat(results.first()).isFocused();

    page.keyboard().press("Escape");
    assertThat(query).isHidden();
    assertThat(trigger).isFocused();
    trigger.click();
    assertThat(query).hasValue("guides cafe");
    query.press("Enter");
    assertThat(query).isHidden();
    Assertions.assertThat(page.url()).endsWith("#cafe-three");
  }
}
