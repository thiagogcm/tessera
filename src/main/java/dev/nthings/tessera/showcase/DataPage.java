package dev.nthings.tessera.showcase;

import static dev.nthings.tessera.core.Elements.text;

import dev.nthings.tessera.components.CodeBlock;
import dev.nthings.tessera.components.DataTable;
import dev.nthings.tessera.components.DataTable.Column;
import dev.nthings.tessera.components.SimpleTable;
import java.util.List;

/// Responsive data tables, simple tables, and code blocks.
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
        simpleTable(),
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
        .features(
            "@container (inline-size < 36rem)", "position: sticky", "tabular-nums", ":nth-child()")
        .example(
            new DataTable<>(
                "services",
                "Service health by region",
                List.of(
                    Column.rowHeader("Service", (ServiceMetric r) -> text(r.service())),
                    Column.text("Region", ServiceMetric::region),
                    Column.numeric("P99 latency", ServiceMetric::latency),
                    Column.numeric("Throughput", ServiceMetric::throughput),
                    Column.numeric("Availability", ServiceMetric::availability)),
                rows))
        .code(
            "new DataTable<>(\"services\", \"Service health by region\", List.of(",
            "    Column.rowHeader(\"Service\", r -> text(r.service())),",
            "    Column.numeric(\"P99 latency\", ServiceMetric::latency)), rows);");
  }

  private static Demo simpleTable() {
    return Demo.of("simple-table", "Simple table")
        .summary("A captioned table for small reference data.")
        .features("<caption>", "border-collapse: separate")
        .example(
            SimpleTable.create()
                .caption("Cascade layers, lowest to highest priority")
                .headers("Layer", "Contents")
                .row("reset", "Box sizing and margins")
                .row("base", "Document defaults and view transitions")
                .row("theme", "Design tokens as custom properties")
                .row("components", "@scope rules per view type")
                .row("utilities", "Content-hashed element styles")
                .row("interactive", "Pseudo-class and motion states")
                .row("container", "Container query overrides"))
        .code(
            "SimpleTable.create().caption(\"…\").headers(\"Layer\", \"Contents\").row(\"reset\", \"…\");");
  }

  private static Demo codeBlock() {
    return Demo.of("code-block", "Code block")
        .summary(
            "A focusable, scrollable <pre> with CSS counter line numbers, highlighted lines, "
                + "+/- diff markers detected automatically, and a zero-JS declarative copy button.")
        .features(
            "counter-increment", "tabindex=\"0\" region", "commandfor copy", "declarative invoker")
        .example(
            CodeBlock.ofLines(
                    "public Element render() {",
                    "-  return div().attr(\"id\", \"dialog\");",
                    "+  var dialog = ElementRef.named(\"dialog\");",
                    "+  return dialog().id(dialog).dialogClosedByAny();",
                    "}")
                .filename("Modal.java")
                .language("java")
                .diffAutoDetect(true)
                .highlightLines(1))
        .code(
            "CodeBlock.ofLines(\"…\", \"-old\", \"+new\")",
            "    .filename(\"Modal.java\").diffAutoDetect(true).highlightLines(1);");
  }
}
