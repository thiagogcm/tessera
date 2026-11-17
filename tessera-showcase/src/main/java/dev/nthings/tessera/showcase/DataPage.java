package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.components.TableGrid.Align.NUMERIC;
import static dev.nthings.tessera.components.TableGrid.Merge.LEFT;
import static dev.nthings.tessera.components.TableGrid.Merge.UP;
import static dev.nthings.tessera.components.TableGrid.body;
import static dev.nthings.tessera.components.TableGrid.columnHeader;
import static dev.nthings.tessera.components.TableGrid.data;
import static dev.nthings.tessera.components.TableGrid.head;
import static dev.nthings.tessera.components.TableGrid.row;
import static dev.nthings.tessera.components.TableGrid.rowHeader;
import static dev.nthings.tessera.components.TableGrid.sectionHeader;

import dev.nthings.tessera.components.CodeBlock;
import dev.nthings.tessera.components.DataTable;
import dev.nthings.tessera.components.TableGrid;
import dev.nthings.tessera.css.CssLength;
import java.util.List;

/// Grid-backed tables, responsive cards, and code blocks.
final class DataPage {

  record ServiceMetric(
      String service, String region, String latency, String throughput, String availability) {}

  private DataPage() {}

  static Page page() {
    return Page.ofDemos(
        "/data",
        "Data & code",
        "Data & code",
        "Tables that turn into labelled cards in narrow containers, compact reference tables, and "
            + "keyboard-scrollable code with line numbers and diffs.",
        dataTable(),
        groupedTable(),
        referenceTable(),
        codeBlock());
  }

  private static Demo dataTable() {
    var rows =
        List.of(
            new ServiceMetric("gateway", "us-east-1", "3.4 ms", "94,200/s", "99.99%"),
            new ServiceMetric("compiler", "eu-central-1", "8.1 ms", "41,800/s", "99.98%"),
            new ServiceMetric("bundler", "ap-northeast-1", "6.2 ms", "52,100/s", "99.97%"),
            new ServiceMetric("audit", "us-west-2", "11.9 ms", "29,400/s", "99.95%"));
    return Demo.of("data-table", "Data table")
        .summary(
            "Sticky column and row headers in a labelled, keyboard-scrollable region. Below 36rem "
                + "of container width, rows become cards whose cells are labelled from the headers. "
                + "Narrow the window to see it.")
        .features("@container (inline-size < 36rem)", "position: sticky", "tabular-nums")
        .example(
            DataTable.of(
                    "Service health by region",
                    TableGrid.of(
                        head(
                            row(
                                columnHeader("Service"),
                                columnHeader("Region"),
                                columnHeader("P99 latency"),
                                columnHeader("Throughput"),
                                columnHeader("Availability"))),
                        body(
                            rows.stream()
                                .map(r ->
                                    row(
                                        rowHeader(r.service()),
                                        data(r.region()),
                                        data(r.latency()).align(NUMERIC),
                                        data(r.throughput()).align(NUMERIC),
                                        data(r.availability()).align(NUMERIC)))
                                .toArray(TableGrid.Row[]::new))))
                .stackBelow(CssLength.rem(36))
                .stickyHead()
                .stickyFirstColumn()
                .id("services"))
        .code(
            "DataTable.of(\"Service health\", TableGrid.of(",
            "    head(row(columnHeader(\"Service\"), columnHeader(\"Latency\"))),",
            "    body(row(rowHeader(\"gateway\"), data(\"3.4 ms\").align(NUMERIC)))))",
            "    .stackBelow(CssLength.rem(36)).stickyHead().stickyFirstColumn();");
  }

  private static Demo groupedTable() {
    return Demo.of("grouped-table", "Grouped table")
        .summary(
            "Two header rows and separate regional sections use OTSL merge slots. "
                + "The table keeps its grid and scrolls horizontally on narrow screens.")
        .features("rowspan", "colspan", "headers", "<tbody>")
        .example(
            DataTable.of(
                    "Quarterly infrastructure spend",
                    TableGrid.of(
                        head(
                            row(
                                columnHeader("Service and region"),
                                columnHeader("2026 spend in USD"),
                                LEFT,
                                LEFT),
                            row(
                                UP,
                                columnHeader("First quarter"),
                                columnHeader("Second quarter"),
                                columnHeader("Year to date"))),
                        body(
                            row(sectionHeader("Americas"), LEFT, LEFT, LEFT),
                            row(
                                rowHeader("Gateway — us-east-1"), data("$12,400").align(NUMERIC),
                                data("$13,100").align(NUMERIC), data("$25,500").align(NUMERIC)),
                            row(
                                rowHeader("Audit — us-west-2"), data("$8,600").align(NUMERIC),
                                data("$9,200").align(NUMERIC), data("$17,800").align(NUMERIC))),
                        body(
                            row(sectionHeader("Europe"), LEFT, LEFT, LEFT),
                            row(
                                rowHeader("Compiler — eu-central-1"),
                                    data("$10,800").align(NUMERIC),
                                data("$11,600").align(NUMERIC), data("$22,400").align(NUMERIC)))))
                .stickyHead()
                .stickyFirstColumn()
                .id("quarterly-spend"))
        .code(
            "TableGrid.of(",
            "    head(row(columnHeader(\"Region\"), columnHeader(\"2026\"), LEFT),",
            "         row(UP, columnHeader(\"Q1\"), columnHeader(\"Q2\"))),",
            "    body(row(rowHeader(\"East\"), data(\"12\"), data(\"15\"))));");
  }

  private static Demo referenceTable() {
    return Demo.of("reference-table", "Reference table")
        .summary("A captioned table for small reference data.")
        .features("<caption>", "border-collapse: separate")
        .example(
            DataTable.of(
                "Cascade layers, lowest to highest priority",
                TableGrid.of(
                    head(row(columnHeader("Layer"), columnHeader("Contents"))),
                    body(
                        row(data("reset"), data("Box sizing and margins")),
                        row(data("base"), data("Document defaults and view transitions")),
                        row(data("theme"), data("Design tokens as custom properties")),
                        row(data("components"), data("@scope rules per scope name")),
                        row(data("utilities"), data("Content-hashed element styles")),
                        row(data("interactive"), data("Pseudo-class and motion states")),
                        row(data("container"), data("Container query overrides"))))))
        .code(
            "DataTable.of(\"Cascade layers\", TableGrid.of(",
            "    head(row(columnHeader(\"Layer\"), columnHeader(\"Contents\"))),",
            "    body(row(data(\"reset\"), data(\"Box sizing and margins\")))));");
  }

  private static Demo codeBlock() {
    return Demo.of("code-block", "Code block")
        .summary(
            "A focusable, scrollable <pre> with CSS counter line numbers, highlighted lines, and "
                + "+/- diff markers detected automatically.")
        .features("counter-increment", "tabindex=\"0\" region", "white-space: pre")
        .example(
            CodeBlock.ofLines(
                    "public IntoElement expand(DocumentContext context) {",
                    "-  return div().attr(\"id\", \"dialog\");",
                    "+  var dialog = ElementRef.scoped(\"dialog\");",
                    "+  return dialog().id(dialog).closedBy(ClosedBy.ANY);",
                    "}")
                .filename("Modal.java")
                .language("java")
                .diffAutoDetect()
                .highlightLines(1))
        .code(
            "CodeBlock.ofLines(\"…\", \"-old\", \"+new\")",
            "    .filename(\"Modal.java\").diffAutoDetect().highlightLines(1);");
  }
}
