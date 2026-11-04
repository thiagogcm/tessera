package dev.nthings.tessera;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.text;
import static org.assertj.core.api.Assertions.assertThat;

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
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.HighlightTone;
import dev.nthings.tessera.css.ScrollReveal;
import dev.nthings.tessera.css.TimelineAxis;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.UnderlineStyle;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PageDocument;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FoundationalComponentsTest {

  record ServiceRow(String service, String region, String p99Latency, String availability) {}

  @Test
  @DisplayName("Compiles responsive DataTable and SimpleTable with container queries")
  void compilesTables() {
    var rows =
        List.of(
            new ServiceRow("auth-edge", "us-east-1", "4.8 ms", "99.99%"),
            new ServiceRow("billing-ledger", "eu-central-1", "11.2 ms", "99.95%"));

    var dataTable =
        new DataTable<>(
            "services",
            "Production service SLO telemetry",
            List.of(
                Column.rowHeader("Service", r -> text(r.service())),
                Column.text("Region", ServiceRow::region),
                Column.numeric("P99 Latency", ServiceRow::p99Latency),
                Column.numeric("Availability", ServiceRow::availability)),
            rows);

    var simple =
        SimpleTable.create()
            .caption("Summary")
            .headers("Metric", "Value")
            .row("Total Services", "2");

    var compiled =
        PageCompiler.compile(new PageDocument("Tables", main().children(dataTable, simple)));

    assertThat(compiled.cssContent())
        .contains("--label-1:\"Service\"")
        .contains("--label-2:\"Region\"")
        .contains("--label-3:\"P99 Latency\"")
        .contains("@container (inline-size < 36rem)")
        .contains(":scope thead{display:none}")
        .contains(":scope td:nth-child(3)::before{content:var(--label-3) \": \"")
        .contains("font-variant-numeric:tabular-nums");

    Document doc = Jsoup.parse(compiled.html());
    var regionWrapper = doc.selectFirst("div[role=region][tabindex=0]");
    assertThat(regionWrapper).isNotNull();
    assertThat(regionWrapper.attr("aria-labelledby")).isEqualTo("datatable-services--caption");

    var caption = doc.getElementById("datatable-services--caption");
    assertThat(caption).isNotNull();
    assertThat(caption.text()).isEqualTo("Production service SLO telemetry");

    var rowHeaders = regionWrapper.select("tbody th[scope=row]");
    assertThat(rowHeaders).hasSize(2);
    assertThat(rowHeaders.getFirst().text()).isEqualTo("auth-edge");
  }

  @Test
  @DisplayName("Compiles cards and subgrid card grid across interaction modes")
  void compilesCardsAndGrids() {
    var mediaCard =
        Card.create("architecture")
            .mediaImg("/assets/arch.webp", "Architecture diagram", "16 / 9")
            .title("Multi-Phase Compiler")
            .subtitle("Three-pass static pipeline")
            .bodyText("Separates layout expansion, accessibility validation, and streaming paint.")
            .stretchedLink("/docs/compiler")
            .action(button().id("bookmark-arch").text("Bookmark"));

    var textOnlyCard =
        Card.create("tokens")
            .title("Reactive Tokens")
            .bodyText("Uses native light-dark() and OKLCH color mixing.")
            .bodyText("Zero runtime theme scripts.")
            .action(Elements.a("/docs/tokens").text("Explore tokens"));

    var choiceCard =
        Card.create("tier-pro")
            .title("Pro Tier")
            .bodyText("Dedicated isolated workers.")
            .choiceRadio("plan", "pro", true);

    var cardGrid =
        new CardGrid(
            "feature-cards",
            CssLength.rem(18),
            CssLength.rem(1.25),
            mediaCard,
            textOnlyCard,
            choiceCard);

    var bentoGrid =
        Grid.columns(
            3,
            CssLength.rem(1),
            Grid.cell(2, 1, div().text("Wide cell")),
            Grid.cell(1, 2, div().text("Tall cell")));

    var compiled =
        PageCompiler.compile(
            new PageDocument("Cards & Grids", main().children(cardGrid, bentoGrid)));

    assertThat(compiled.cssContent())
        .contains("grid-template-rows:subgrid;grid-row:span 4")
        .contains(":scope:has(> img,> picture,> .media)")
        .contains(":scope:has(a.card-link) a.card-link::after")
        .contains(":scope:has(> input:checked)");

    Document doc = Jsoup.parse(compiled.html());

    // Media card has direct <img>, <hgroup>, single <p> (unwrapped), and <footer>
    var firstArticle =
        doc.selectFirst("article[data-view^=card-][data-view-id=feature-cards--card-architecture]");
    assertThat(firstArticle).isNotNull();
    assertThat(firstArticle.children().get(0).tagName()).isEqualTo("img");
    assertThat(firstArticle.children().get(1).tagName()).isEqualTo("hgroup");
    assertThat(firstArticle.children().get(2).tagName()).isEqualTo("p");
    assertThat(firstArticle.children().get(3).tagName()).isEqualTo("footer");
    assertThat(firstArticle.selectFirst("hgroup h3 a.card-link")).isNotNull();

    // Text-only card with 2 paragraphs groups content in a single <div> for subgrid track alignment
    var secondArticle =
        doc.selectFirst("article[data-view^=card-][data-view-id=feature-cards--card-tokens]");
    assertThat(secondArticle).isNotNull();
    assertThat(secondArticle.children().get(0).tagName()).isEqualTo("h3");
    assertThat(secondArticle.children().get(1).tagName()).isEqualTo("div");

    // Choice card uses <label> root with direct <input type="radio" checked>
    var choiceLabel =
        doc.selectFirst("label[data-view^=card-][data-view-id=feature-cards--card-tier-pro]");
    assertThat(choiceLabel).isNotNull();
    assertThat(choiceLabel.selectFirst("> input[type=radio][checked]")).isNotNull();
  }

  @Test
  @DisplayName("Compiles scroll-driven animations with reduced motion gating")
  void compilesScrollDrivenAnimations() {
    var progress = new ScrollProgressBar("top-progress");
    var revealSection =
        Elements.section()
            .viewTimeline("hero-tl", TimelineAxis.BLOCK)
            .scrollReveal(ScrollReveal.FADE_UP)
            .staggerBySiblingIndex(80)
            .child(p().text("Revealed on scroll"));

    var compiled =
        PageCompiler.compile(
            new PageDocument("Scroll Animations", main().children(progress, revealSection)));

    String css = compiled.cssContent();
    assertThat(css)
        .contains(
            "@keyframes tessera-scroll-progress{from{transform:scaleX(0)}to{transform:scaleX(1)}}")
        .contains("@keyframes tessera-fade-up{")
        .contains("@media (prefers-reduced-motion: no-preference){")
        .contains("animation:tessera-fade-up auto linear backwards;")
        .contains("animation-delay:calc((sibling-index() - 1) * 80ms);")
        .contains("animation-timeline:view(block);")
        .contains("animation-range:entry 0% entry 100%");

    // Verify animation shorthand precedes animation-timeline
    int animIdx = css.indexOf("animation:tessera-scroll-progress auto linear");
    int timelineIdx = css.indexOf("animation-timeline:scroll(root block)");
    assertThat(animIdx).isGreaterThan(0).isLessThan(timelineIdx);

    Document doc = Jsoup.parse(compiled.html());
    var progressBar = doc.getElementById("top-progress");
    assertThat(progressBar).isNotNull();
    assertThat(progressBar.attr("aria-hidden")).isEqualTo("true");
  }

  @Test
  @DisplayName("Compiles semantic lists, inline formatting, and hoisted footnotes")
  void compilesListsFormattingAndFootnotes() {
    var prose =
        p().children(
                text("Tessera supports "),
                Formatting.highlight("OKLCH highlights", HighlightTone.ACCENT),
                text(", "),
                Formatting.underline(
                    "wavy spellcheck underlines", UnderlineStyle.WAVY, Tokens.STATUS_DANGER),
                text(", "),
                Formatting.strikethrough("deprecated legacy shims"),
                text(", "),
                Formatting.deleted("old API"),
                text(" -> "),
                Formatting.inserted("new API"),
                text(", and press "),
                Formatting.keybinding("Ctrl+K"),
                Footnotes.ref("fn-1", "First footnote with instant interestfor preview."),
                text(" and "),
                Footnotes.ref("fn-2", "Second footnote auto-numbered in traversal order."));

    var bulletList = BulletList.ofStrings("Zero warning suppressions", "JDK 25 ScopedValue");
    var numberedList = NumberedList.ofStrings("Phase 1: requestLayout", "Phase 2: prepaint");
    var descriptionList =
        DescriptionList.of(
            Term.of("Runtime", "JDK 25 LTS"),
            Term.of("Browser Target", "Chrome 156+ (Baseline 2026)"));
    var stepList = StepList.ofStrings("onboarding", "Install JDK 25", "Run just verify");

    var compiled =
        PageCompiler.compile(
            new PageDocument(
                "Prose, Lists & Footnotes",
                main().children(prose, bulletList, numberedList, descriptionList, stepList)));

    assertThat(compiled.cssContent())
        .contains("color-mix(in oklch, var(--accent-primary) 24%, transparent)")
        .contains("text-decoration-line:underline")
        .contains("text-decoration-style:wavy")
        .contains("text-decoration-skip-ink:auto")
        .contains("text-decoration-line:line-through")
        .contains("--step-index:sibling-index();--step-total:sibling-count()");

    Document doc = Jsoup.parse(compiled.html());
    assertThat(doc.selectFirst("mark")).isNotNull();
    assertThat(doc.selectFirst("u")).isNotNull();
    assertThat(doc.selectFirst("s")).isNotNull();
    assertThat(doc.selectFirst("del")).isNotNull();
    assertThat(doc.selectFirst("ins")).isNotNull();
    assertThat(doc.selectFirst("kbd")).isNotNull();
    assertThat(doc.select("dl > dt")).hasSize(2);
    assertThat(doc.select("dl > dd")).hasSize(2);

    // Verify inline footnote references have sequential numbers + adjacent interestfor tooltips
    var firstRefSup = doc.getElementById("fnref-fn-1");
    assertThat(firstRefSup).isNotNull();
    var firstRefLink = firstRefSup.selectFirst("a[role=doc-noteref]");
    assertThat(firstRefLink).isNotNull();
    assertThat(firstRefLink.text()).isEqualTo("[1]");
    assertThat(firstRefLink.attr("href")).isEqualTo("#fn-fn-1");
    assertThat(firstRefLink.nextElementSibling()).isNotNull();
    assertThat(firstRefLink.nextElementSibling().attr("popover")).isEqualTo("hint");

    // Verify auto-hoisted <aside role="doc-endnotes"> at end of body with bidirectional links
    var endnotes = doc.selectFirst("aside[role=doc-endnotes]");
    assertThat(endnotes).isNotNull();
    var noteItems = endnotes.select("ol > li[role=doc-footnote]");
    assertThat(noteItems).hasSize(2);
    assertThat(noteItems.get(0).id()).isEqualTo("fn-fn-1");
    var backlink = noteItems.get(0).selectFirst("a[role=doc-backlink]");
    assertThat(backlink).isNotNull();
    assertThat(backlink.attr("href")).isEqualTo("#fnref-fn-1");
  }
}
