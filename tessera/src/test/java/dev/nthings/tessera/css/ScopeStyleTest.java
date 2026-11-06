package dev.nthings.tessera.css;

import static dev.nthings.tessera.core.Elements.text;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.components.Card;
import dev.nthings.tessera.components.DataTable;
import dev.nthings.tessera.components.DataTable.Column;
import dev.nthings.tessera.support.RenderedPage;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("ScopeStyle")
class ScopeStyleTest {

  private static RenderedPage page;

  @BeforeAll
  static void renderViewsSharingScopedCss() {
    var columns =
        List.of(
            Column.<String>rowHeader("Key", s -> text(s)), Column.<String>text("Value", s -> s));
    page =
        renderMain(
            Card.create("one").title("First").bodyText("Body 1"),
            Card.create("two").title("Second").bodyText("Body 2"),
            new DataTable<>("a", "A", columns, List.of("row-a")),
            new DataTable<>("b", "B", columns, List.of("row-b", "row-c")));
  }

  @ParameterizedTest(name = "{0} and {1}")
  @CsvSource({"card-one, card-two", "datatable-a, datatable-b"})
  @DisplayName("views with identical scoped CSS share one data-view key and one @scope block")
  void identicalScopedCssIsDeduplicated(String firstView, String secondView) {
    var key = page.element("[data-view-id=" + firstView + "]").attr("data-view");

    assertThat(page.element("[data-view-id=" + secondView + "]")).hasAttribute("data-view", key);
    var scope = "@scope ([data-view=\"" + key + "\"])";
    assertThat(page.css()).containsOnlyOnce(scope);
  }
}
