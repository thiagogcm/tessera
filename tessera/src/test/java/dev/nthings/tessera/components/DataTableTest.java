package dev.nthings.tessera.components;

import static dev.nthings.tessera.components.TableGrid.Align.NUMERIC;
import static dev.nthings.tessera.components.TableGrid.Merge.BOTH;
import static dev.nthings.tessera.components.TableGrid.Merge.LEFT;
import static dev.nthings.tessera.components.TableGrid.Merge.UP;
import static dev.nthings.tessera.components.TableGrid.body;
import static dev.nthings.tessera.components.TableGrid.columnHeader;
import static dev.nthings.tessera.components.TableGrid.cornerHeader;
import static dev.nthings.tessera.components.TableGrid.data;
import static dev.nthings.tessera.components.TableGrid.empty;
import static dev.nthings.tessera.components.TableGrid.foot;
import static dev.nthings.tessera.components.TableGrid.head;
import static dev.nthings.tessera.components.TableGrid.row;
import static dev.nthings.tessera.components.TableGrid.rowHeader;
import static dev.nthings.tessera.components.TableGrid.sectionHeader;
import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.code;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.support.RenderedPage;
import java.util.Arrays;
import java.util.List;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DataTable")
class DataTableTest {

  private static TableGrid sales;
  private static RenderedPage page;

  @BeforeAll
  static void renderTables() {
    sales =
        TableGrid.of(
            head(
                row(cornerHeader("Area"), LEFT, columnHeader("2026"), LEFT),
                row(UP, BOTH, columnHeader("Q1"), columnHeader("Q2"))),
            body(
                row(sectionHeader("Americas"), LEFT, LEFT, LEFT),
                row(rowHeader("US"), rowHeader("East"), data("12").align(NUMERIC), data("15")),
                row(UP, rowHeader("West"), empty(), data("9"))),
            body(
                row(sectionHeader("Europe"), LEFT, LEFT, LEFT),
                row(rowHeader("DE"), rowHeader("Central"), data("8"), data("11"))),
            foot(row(rowHeader("Total"), LEFT, data("20"), data("35"))));
    page =
        renderMain(
            DataTable.of("Sales", sales).id("sales"), DataTable.of("Sales copy", sales).id("copy"));
  }

  @Test
  @DisplayName("emits anchors once with spans and preserves all row groups")
  void emitsAnchorsAndGroups() {
    var table = page.element("main > div > table");
    assertThat(table.selectFirst("thead th"))
        .hasAttribute("rowspan", "2")
        .hasAttribute("colspan", "2");
    assertThat(table.select("thead > tr").get(1).children()).hasSize(2);
    assertThat(table.select("tbody")).hasSize(2);
    assertThat(table.select("tbody").getFirst().select("tr").get(2).children()).hasSize(3);
    assertThat(table.selectFirst("tfoot th")).hasAttribute("colspan", "2").hasText("Total");
    assertThat(table.select("tbody td:empty")).hasSize(1);
    assertThat(page.element("div[role=region][tabindex=0]"))
        .hasAttribute("aria-labelledby", "sales--caption");
  }

  @Test
  @DisplayName("links grouped column and row headers without leaking between body groups")
  void associatesHeaders() {
    assertThat(headerTexts(page.elementById("sales--cell-3-0"))).containsExactly("Americas");
    assertThat(headerTexts(page.element("table tbody td[data-column=\"2\"]")))
        .containsExactly("2026", "Q1", "Americas", "US", "East");
    assertThat(headerTexts(page.element("table tbody").select("tr").get(2).selectFirst("td")))
        .containsExactly("2026", "Q1", "Americas", "US", "West");
    var europe = page.element("table").select("tbody").get(1).selectFirst("td");
    assertThat(headerTexts(europe)).containsExactly("2026", "Q1", "Europe", "DE", "Central");
    assertThat(headerTexts(page.element("tfoot td"))).containsExactly("2026", "Q1", "Total");
  }

  @Test
  @DisplayName("reused grids bind headers inside each table instance")
  void isolatesReferencesAcrossPlacements() {
    var tables = page.elements("main > div > table");
    for (var table : tables) {
      for (var cell : table.select("[headers]")) {
        for (var id : cell.attr("headers").split(" ")) {
          assertThat(page.elementById(id).closest("table")).isSameAs(table);
        }
      }
    }
    assertThat(page.element("tbody td[data-align=numeric]"))
        .hasAttribute("data-column", "2")
        .hasText("12");
  }

  @Test
  @DisplayName("keeps rich and nested content in a headerless grid")
  void preservesRichAndNestedContent() {
    var nested = DataTable.of("Nested", TableGrid.of(body(row(data("Inner")))));
    var rich =
        renderMain(
            DataTable.of(
                "Reference",
                TableGrid.of(
                    body(
                        row(
                            data(a("https://example.com").text("Docs")),
                            data(code().text("a < b"))),
                        row(empty(), data(nested))))));

    var table = rich.element("main > div > table");
    assertThat(table.selectFirst("tbody > tr > td > a"))
        .hasAttribute("href", "https://example.com")
        .hasText("Docs");
    assertThat(table.selectFirst("code")).hasText("a < b");
    assertThat(table.select("thead")).isEmpty();
    assertThat(table.selectFirst("td table caption")).hasText("Nested");
  }

  @Test
  @DisplayName("card labels are escaped HTML and preserve rich column headings")
  void rendersCardLabelsAsContent() {
    var flat =
        TableGrid.of(
            head(
                row(
                    columnHeader("Service"),
                    columnHeader("Region \"AZ\" <zone>", code().text("Region \"AZ\" <zone>")))),
            body(row(rowHeader("gateway"), data("us-east-1"))));
    var cards = renderMain(DataTable.of("Telemetry", flat).stackBelow(CssLength.rem(36)));

    assertThat(cards.element("thead code")).hasText("Region \"AZ\" <zone>");
    assertThat(cards.element("td > .table-cell-label"))
        .hasAttribute("aria-hidden", "true")
        .hasText("Region \"AZ\" <zone>:");
    assertThat(cards.elements("tbody th .table-cell-label, td zone")).isEmpty();
    assertThat(cards.css()).doesNotContain("Region", "nth-child");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DataTable.of("Sales", sales).stackBelow(CssLength.rem(36)))
        .withMessageContaining("flat grid");
  }

  private static List<String> headerTexts(Element cell) {
    return Arrays.stream(cell.attr("headers").split(" "))
        .map(id -> page.elementById(id).text())
        .toList();
  }
}
