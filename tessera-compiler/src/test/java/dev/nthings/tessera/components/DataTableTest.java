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
            DataTable.of("Sales", sales).scopeId("sales"),
            DataTable.of("Sales copy", sales).scopeId("copy"));
  }

  private static Element firstTable() {
    return page.element("table");
  }

  private static Element cell(String text) {
    return firstTable().select("th, td").stream()
        .filter(cell -> cell.text().equals(text))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No cell " + text));
  }

  private static List<String> headerTexts(Element cell) {
    return page.referenced(cell, "headers").stream().map(Element::text).toList();
  }

  @Test
  @DisplayName("a focusable region named by the caption holds merged cells and every row group")
  void emitsSpansAndGroups() {
    var region = page.element("[role=region]");
    assertThat(region).hasAttribute("tabindex", "0");
    assertThat(page.target(region, "aria-labelledby")).hasTagName("caption").hasText("Sales");

    var table = firstTable();
    assertThat(cell("Area")).hasAttribute("rowspan", "2").hasAttribute("colspan", "2");
    assertThat(cell("2026")).hasAttribute("colspan", "2");
    assertThat(cell("US")).hasAttribute("rowspan", "2");
    assertThat(table.select("tbody")).hasSize(2);
    assertThat(table.select("tbody td:empty")).hasSize(1);
    assertThat(table.selectFirst("tfoot th")).hasAttribute("colspan", "2").hasText("Total");
  }

  @Test
  @DisplayName("each cell names its column, section and row headers without leaking across bodies")
  void associatesHeaders() {
    assertThat(headerTexts(cell("US"))).containsExactly("Americas");
    assertThat(headerTexts(cell("12"))).containsExactly("2026", "Q1", "Americas", "US", "East");
    assertThat(headerTexts(cell("9"))).containsExactly("2026", "Q2", "Americas", "US", "West");
    assertThat(headerTexts(firstTable().selectFirst("tbody td:empty")))
        .containsExactly("2026", "Q1", "Americas", "US", "West");
    assertThat(headerTexts(cell("8"))).containsExactly("2026", "Q1", "Europe", "DE", "Central");
    assertThat(headerTexts(cell("20"))).containsExactly("2026", "Q1", "Total");
  }

  @Test
  @DisplayName("a grid placed twice binds headers inside each table")
  void isolatesReferencesAcrossPlacements() {
    var tables = page.elements("table");
    assertThat(tables).hasSize(2);
    for (var table : tables) {
      for (var cell : table.select("[headers]")) {
        assertThat(page.referenced(cell, "headers"))
            .allSatisfy(header -> assertThat(header.closest("table")).isSameAs(table));
      }
    }
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

    var table = rich.element("table");
    assertThat(table.selectFirst("td > a"))
        .hasAttribute("href", "https://example.com")
        .hasText("Docs");
    assertThat(table.selectFirst("code")).hasText("a < b");
    assertThat(table.select("thead")).isEmpty();
    assertThat(table.selectFirst("td table caption")).hasText("Nested");
  }

  @Test
  @DisplayName("stacked cards label data cells with their column heading as hidden text")
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
    assertThat(cards.element("tbody td [aria-hidden=true]")).hasText("Region \"AZ\" <zone>:");
    assertThat(cards.element("tbody td")).hasText("Region \"AZ\" <zone>: us-east-1");
    assertThat(cards.elements("tbody th [aria-hidden=true], zone")).isEmpty();
  }

  @Test
  @DisplayName("stacked cards need a flat grid")
  void rejectsStackingMergedGrid() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DataTable.of("Sales", sales).stackBelow(CssLength.rem(36)));
  }
}
