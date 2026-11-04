package dev.nthings.tessera.site;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.deferred;
import static dev.nthings.tessera.core.Elements.dialog;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.footer;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.header;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.section;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.core.Elements.text;

import dev.nthings.tessera.components.Card;
import dev.nthings.tessera.components.CardGrid;
import dev.nthings.tessera.components.DataTable;
import dev.nthings.tessera.components.DataTable.Column;
import dev.nthings.tessera.components.Footnotes;
import dev.nthings.tessera.components.Formatting;
import dev.nthings.tessera.components.Grid;
import dev.nthings.tessera.components.Lists.BulletList;
import dev.nthings.tessera.components.Lists.DescriptionList;
import dev.nthings.tessera.components.Lists.NumberedList;
import dev.nthings.tessera.components.Lists.StepList;
import dev.nthings.tessera.components.Lists.Term;
import dev.nthings.tessera.components.ScrollProgressBar;
import dev.nthings.tessera.components.SimpleTable;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.HighlightTone;
import dev.nthings.tessera.css.ScrollReveal;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.UnderlineStyle;
import dev.nthings.tessera.render.PageDocument;
import java.nio.file.Path;
import java.util.List;

/**
 * Multi-page flagship showcase site demonstrating Tessera's Chrome 156+ (Baseline 2026+)
 * capabilities across {@code /}, {@code /telemetry}, and {@code /typography}.
 */
public final class ShowcaseSite {

  public record ServiceMetric(
      String service, String region, String p99Latency, String throughput, String availability) {}

  record ShellLayout(String activePath, String pageHeading, String subtitle, Element content)
      implements RenderOnce {

    @Override
    public Element render() {
      var navBar =
          header()
              .sticky()
              .insetBlockStart(CssLength.px(0))
              .zIndex(20)
              .bg(Tokens.SURFACE_CARD)
              .borderBlockEndSubtle(Tokens.BORDER_SUBTLE)
              .px(CssLength.rem(2))
              .py(CssLength.rem(1))
              .flex()
              .itemsCenter()
              .justifyBetween()
              .children(
                  span().fontWeight(700).textSize(CssLength.rem(1.125)).text("Tessera // JDK 25"),
                  nav()
                      .ariaLabel("Primary navigation")
                      .flex()
                      .gap(CssLength.rem(1.25))
                      .children(
                          navLink("/", "Overview & Cards", activePath),
                          navLink("/telemetry", "Telemetry & DataTables", activePath),
                          navLink("/typography", "Typography & Footnotes", activePath)));

      var hero =
          section()
              .flex()
              .flexCol()
              .gap(CssLength.rem(0.5))
              .py(CssLength.rem(1.5))
              .children(
                  h1().textSize(
                          CssLength.fluid(
                              CssLength.rem(1.75), CssLength.cqi(4), CssLength.rem(2.75)))
                      .textBalance()
                      .text(pageHeading),
                  p().textColor(Tokens.TEXT_MUTED)
                      .textSize(CssLength.rem(1.125))
                      .textPretty()
                      .text(subtitle));

      return div()
          .children(
              new ScrollProgressBar("page-reading-progress"),
              navBar,
              Elements.main()
                  .maxInlineSize(CssLength.rem(72))
                  .mx(CssLength.auto())
                  .px(CssLength.rem(2))
                  .py(CssLength.rem(2))
                  .flex()
                  .flexCol()
                  .gap(CssLength.rem(2.5))
                  .children(hero, content),
              footer()
                  .maxInlineSize(CssLength.rem(72))
                  .mx(CssLength.auto())
                  .px(CssLength.rem(2))
                  .py(CssLength.rem(2))
                  .borderBlockStartSubtle(Tokens.BORDER_SUBTLE)
                  .textColor(Tokens.TEXT_MUTED)
                  .textSize(CssLength.rem(0.875))
                  .text("Compiled with Tessera on JDK 25 targeting Chrome 156+ (Baseline 2026)."));
    }

    private static Element navLink(String href, String label, String activePath) {
      boolean active = href.equals(activePath);
      return a(href)
          .px(CssLength.rem(0.75))
          .py(CssLength.rem(0.375))
          .rounded(CssLength.rem(0.5))
          .fontWeight(active ? 600 : 400)
          .whenElse(
              active,
              el -> el.bg(Tokens.SURFACE_SUBTLE).textColor(Tokens.ACCENT_PRIMARY),
              el -> el.textColor(Tokens.TEXT_PRIMARY).hover(s -> s.bg(Tokens.SURFACE_SUBTLE)))
          .text(label);
    }
  }

  private ShowcaseSite() {}

  public static SiteDefinition createDefinition() {
    return new SiteDefinition(
        new SiteRoute("/", buildOverviewPage()),
        new SiteRoute("/telemetry", buildTelemetryPage()),
        new SiteRoute("/typography", buildTypographyPage()));
  }

  private static PageDocument buildOverviewPage() {
    var inspectModal =
        dialog()
            .id("architecture-modal")
            .dialogClosedByAny()
            .p(CssLength.rem(1.75))
            .maxInlineSize(CssLength.rem(32))
            .rounded(CssLength.rem(0.75))
            .bg(Tokens.SURFACE_CARD)
            .borderSubtle(Tokens.BORDER_SUBTLE)
            .children(
                h2().textBalance().text("Three-Phase Static Pipeline"),
                p().py(CssLength.rem(0.75))
                    .textPretty()
                    .text(
                        "Phase 1 expands RenderOnce and RenderView trees, Phase 2 verifies WCAG and Invoker Command integrity, and Phase 3 streams HTML/CSS."),
                button()
                    .id("close-architecture-modal")
                    .command("architecture-modal", BuiltinCommand.REQUEST_CLOSE)
                    .px(CssLength.rem(1))
                    .py(CssLength.rem(0.5))
                    .rounded(CssLength.rem(0.5))
                    .bg(Tokens.ACCENT_PRIMARY)
                    .textColorRaw("white")
                    .cursorPointer()
                    .text("Close Dialog"));

    var cardGrid =
        new CardGrid(
            "overview-cards",
            CssLength.rem(19),
            CssLength.rem(1.25),
            Card.create("telemetry-card")
                .title("Responsive DataTables")
                .subtitle("Container-adaptive tabular layouts")
                .bodyText(
                    "Sticky row and column headers that automatically collapse into labeled cards inside narrow containers.")
                .stretchedLink("/telemetry")
                .action(
                    button()
                        .id("open-arch-modal")
                        .command(inspectModal, BuiltinCommand.SHOW_MODAL)
                        .interestTooltip(
                            tip ->
                                tip.p(CssLength.rem(0.5))
                                    .bg(Tokens.SURFACE_SUBTLE)
                                    .rounded(CssLength.rem(0.375))
                                    .text("Opens native <dialog closedby=\"any\">"))
                        .px(CssLength.rem(0.75))
                        .py(CssLength.rem(0.375))
                        .rounded(CssLength.rem(0.375))
                        .borderSubtle(Tokens.BORDER_SUBTLE)
                        .bg(Tokens.SURFACE_SUBTLE)
                        .cursorPointer()
                        .text("Inspect Pipeline"))
                .action(deferred(inspectModal, 10)),
            Card.create("typography-card")
                .title("Typography, Lists & Footnotes")
                .subtitle("Digital publishing & CSS Text Decoration L4")
                .bodyText(
                    "Auto-numbered footnotes with interestfor hover previews, sibling-index() step lists, and OKLCH highlights.")
                .stretchedLink("/typography")
                .action(span().textColor(Tokens.TEXT_MUTED).text("Zero JavaScript")),
            Card.create("edition-choice")
                .title("Baseline 2026 Target")
                .subtitle("Chrome 156+ native runtime")
                .bodyText(
                    "Select this card to test :has(> input:checked) styling without layout shift.")
                .choiceCheckbox("baseline-opt-in", "chrome-156", true));

    var body =
        new ShellLayout(
            "/",
            "Declarative HTML & CSS View Engine for JDK 25",
            "Multi-phase static compilation backed by @layer, @scope, @container, and native Invoker Commands.",
            div().flex().flexCol().gap(CssLength.rem(2)).children(cardGrid));

    return new PageDocument("Tessera — Overview", body);
  }

  private static PageDocument buildTelemetryPage() {
    var metrics =
        List.of(
            new ServiceMetric("gateway-edge", "us-east-1", "3.4 ms", "94,200 req/s", "99.99%"),
            new ServiceMetric(
                "compiler-worker", "eu-central-1", "8.1 ms", "41,800 req/s", "99.98%"),
            new ServiceMetric(
                "asset-bundler", "ap-northeast-1", "6.2 ms", "52,100 req/s", "99.97%"),
            new ServiceMetric("audit-stream", "us-west-2", "11.9 ms", "29,400 req/s", "99.95%"));

    var dataTable =
        new DataTable<>(
            "service-slo",
            "Regional Service SLO Telemetry",
            List.of(
                Column.rowHeader("Service", r -> text(r.service())),
                Column.text("Region", ServiceMetric::region),
                Column.numeric("P99 Latency", ServiceMetric::p99Latency),
                Column.numeric("Throughput", ServiceMetric::throughput),
                Column.numeric("Availability", ServiceMetric::availability)),
            metrics);

    var bentoSummary =
        Grid.columns(
            3,
            CssLength.rem(1),
            Grid.cell(
                    2,
                    1,
                    div()
                        .p(CssLength.rem(1.25))
                        .rounded(CssLength.rem(0.75))
                        .bg(Tokens.SURFACE_CARD)
                        .borderSubtle(Tokens.BORDER_SUBTLE)
                        .scrollReveal(ScrollReveal.FADE_UP)
                        .staggerBySiblingIndex(60)
                        .children(
                            h2().textSize(CssLength.rem(1.125)).text("Global Fleet Health"),
                            p().textColor(Tokens.TEXT_MUTED)
                                .text("All 4 regional clusters operating within error budget.")))
                .intoElement(),
            Grid.cell(
                    1,
                    1,
                    SimpleTable.create()
                        .caption("Cascade Budget")
                        .headers("Layer", "Rules")
                        .row("components", "Deduplicated")
                        .row("utilities", "FNV-1a hashed"))
                .scrollReveal(ScrollReveal.FADE_UP)
                .staggerBySiblingIndex(60));

    var body =
        new ShellLayout(
            "/telemetry",
            "Telemetry & Responsive DataTables",
            "Resize the viewport or container below 36rem to observe the @container stacked-card transformation.",
            div().flex().flexCol().gap(CssLength.rem(2)).children(dataTable, bentoSummary));

    return new PageDocument("Tessera — Telemetry", body);
  }

  private static PageDocument buildTypographyPage() {
    var proseSection =
        section()
            .p(CssLength.rem(1.5))
            .rounded(CssLength.rem(0.75))
            .bg(Tokens.SURFACE_CARD)
            .borderSubtle(Tokens.BORDER_SUBTLE)
            .flex()
            .flexCol()
            .gap(CssLength.rem(1))
            .children(
                h2().textSize(CssLength.rem(1.25))
                    .text("Inline Formatting & Interactive Citations"),
                p().textPretty()
                    .children(
                        text("Tessera combines "),
                        Formatting.highlight("OKLCH adaptive highlights", HighlightTone.ACCENT),
                        text(", "),
                        Formatting.underline(
                            "CSS Text Decoration Level 4 wavy underlines",
                            UnderlineStyle.WAVY,
                            Tokens.STATUS_WARNING),
                        text(", "),
                        Formatting.strikethrough("legacy polyfill shims"),
                        text(", diff markers ("),
                        Formatting.deleted("String viewScopeRule()"),
                        text(" -> "),
                        Formatting.inserted("void configureScope(ScopeStyle)"),
                        text("), inline code "),
                        Formatting.inlineCode("ScopedValue<RenderContext>"),
                        text(", and keyboard shortcuts like "),
                        Formatting.keybinding("Ctrl+Shift+P"),
                        Footnotes.ref(
                            "scoped-values",
                            "JEP 506 Scoped Values propagate RenderContext lexically without parameter drilling."),
                        text(" alongside native CSS Cascade Layers"),
                        Footnotes.ref(
                            "cascade-layers",
                            "The 7-layer cascade guarantees @layer interactive and @layer container override @layer utilities.")));

    var listsGrid =
        Grid.autoFit(
            CssLength.rem(20),
            CssLength.rem(1.25),
            div()
                .p(CssLength.rem(1.25))
                .rounded(CssLength.rem(0.75))
                .bg(Tokens.SURFACE_CARD)
                .borderSubtle(Tokens.BORDER_SUBTLE)
                .flex()
                .flexCol()
                .gap(CssLength.rem(0.75))
                .children(
                    h2().textSize(CssLength.rem(1.125)).text("Sibling-Index Step List"),
                    StepList.ofStrings(
                        "pipeline-steps",
                        "Phase 1: requestLayout (Expand & Intern)",
                        "Phase 2: prepaint (WCAG & Link Integrity)",
                        "Phase 3: paint (Streaming OWASP Encode)")),
            div()
                .p(CssLength.rem(1.25))
                .rounded(CssLength.rem(0.75))
                .bg(Tokens.SURFACE_CARD)
                .borderSubtle(Tokens.BORDER_SUBTLE)
                .flex()
                .flexCol()
                .gap(CssLength.rem(0.75))
                .children(
                    h2().textSize(CssLength.rem(1.125)).text("Platform Specifications"),
                    DescriptionList.of(
                        Term.of("Java Baseline", "JDK 25 LTS"),
                        Term.of("Browser Target", "Chrome 156+"),
                        Term.of("Nullness Contract", "JSpecify @NullMarked + NullAway")),
                    BulletList.ofStrings("Content-addressed @scope", "Shared CSS site bundle"),
                    NumberedList.ofStrings("Zero preview flags", "Zero warning suppressions")));

    var body =
        new ShellLayout(
            "/typography",
            "Typography, Lists & Dual-Mode Footnotes",
            "Hover or focus the [1] and [2] superscripts for instant interestfor previews, or click to jump to endnotes.",
            div().flex().flexCol().gap(CssLength.rem(2)).children(proseSection, listsGrid));

    return new PageDocument("Tessera — Typography & Footnotes", body);
  }

  public static void main(String[] args) throws InterruptedException {
    Path outputDir = Path.of("target/site");
    boolean serve = false;
    int port = 8080;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--output" -> {
          if (i + 1 < args.length) {
            outputDir = Path.of(args[++i]);
          }
        }
        case "--serve" -> {
          serve = true;
          if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
            port = Integer.parseInt(args[++i]);
          }
        }
        default -> {}
      }
    }

    SiteCompiler.CompiledSite compiled = SiteCompiler.compile(createDefinition());
    SiteCompiler.writeToDirectory(compiled, outputDir);
    System.out.println(
        "Compiled "
            + compiled.pagesByRoute().size()
            + " routes + shared stylesheet ("
            + compiled.sharedCssPath()
            + ") to "
            + outputDir.toAbsolutePath());

    if (serve) {
      try (var server = SitePreviewServer.start(compiled, port)) {
        System.out.println("Serving Tessera showcase at " + server.baseUrl() + " (Ctrl+C to stop)");
        Thread.currentThread().join();
      }
    }
  }
}
