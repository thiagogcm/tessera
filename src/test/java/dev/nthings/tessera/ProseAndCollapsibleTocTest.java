package dev.nthings.tessera;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.nthings.tessera.components.ContentSection;
import dev.nthings.tessera.components.Prose;
import dev.nthings.tessera.components.Toc;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PageDocument;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class ProseAndCollapsibleTocTest {

  @Test
  void proseRendersWithMaxMeasureAndTypographyScope() {
    var prose =
        Prose.of(
            p().text("First paragraph of narrative prose."),
            p().text("Second paragraph with editorial analysis."));

    var doc = new PageDocument("Prose Test", main().child(prose));
    var compiled = PageCompiler.compile(doc);
    var parsed = Jsoup.parse(compiled.html());

    var canvas = parsed.selectFirst(".prose-canvas");
    assertThat(canvas).isNotNull();
    assertThat(canvas.attr("data-view")).isEqualTo("prose-container");

    var paragraphs = canvas.select("p");
    assertThat(paragraphs).hasSize(2);
    assertThat(paragraphs.get(0).text()).isEqualTo("First paragraph of narrative prose.");

    // Scope rules must contain 75ch measure and fluid typography
    assertThat(compiled.cssContent()).contains("max-inline-size:75ch");
    assertThat(compiled.cssContent()).contains("p:not([class])");
    assertThat(compiled.cssContent()).contains("line-height:1.75");
  }

  @Test
  void proseCustomMeasure() {
    var prose =
        Prose.create("custom-prose")
            .maxMeasure(CssLength.ch(65))
            .child(p().text("Narrower measure text."));

    var doc = new PageDocument("Custom Measure Test", main().child(prose));
    var compiled = PageCompiler.compile(doc);

    assertThat(compiled.cssContent()).contains("max-inline-size:65ch");
  }

  @Test
  void contentSectionSurfacesAndSemanticStructure() {
    var ghost =
        ContentSection.ghost("sec-ghost", "Ghost Section")
            .badge("EDITORIAL")
            .leadText("Lead paragraph for ghost surface.")
            .child(p().text("Body inside ghost section."));

    var card =
        ContentSection.card("sec-card", "Card Section")
            .badge("WIDGET")
            .child(p().text("Body inside card section."));

    var highlight =
        ContentSection.highlight("sec-highlight", "Highlight Section")
            .level(3)
            .child(p().text("Body inside highlight section."));

    var doc = new PageDocument("Section Surfaces Test", main().children(ghost, card, highlight));
    var compiled = PageCompiler.compile(doc);
    var parsed = Jsoup.parse(compiled.html());

    // 1. Ghost section
    var ghostEl = parsed.selectFirst("section#sec-ghost");
    assertThat(ghostEl).isNotNull();
    assertThat(ghostEl.attr("data-surface")).isEqualTo("ghost");
    assertThat(ghostEl.selectFirst(".section-badge")).isNotNull();
    assertThat(ghostEl.selectFirst(".section-badge").text()).isEqualTo("EDITORIAL");
    assertThat(ghostEl.selectFirst(".section-lead")).isNotNull();
    assertThat(ghostEl.selectFirst(".section-lead").text())
        .isEqualTo("Lead paragraph for ghost surface.");
    assertThat(ghostEl.selectFirst("h2#sec-ghost--heading")).isNotNull();
    assertThat(ghostEl.selectFirst("h2#sec-ghost--heading").text()).isEqualTo("Ghost Section");

    // 2. Card section
    var cardEl = parsed.selectFirst("section#sec-card");
    assertThat(cardEl).isNotNull();
    assertThat(cardEl.attr("data-surface")).isEqualTo("card");
    assertThat(cardEl.selectFirst("h2#sec-card--heading")).isNotNull();

    // 3. Highlight section with level 3 (h3)
    var highlightEl = parsed.selectFirst("section#sec-highlight");
    assertThat(highlightEl).isNotNull();
    assertThat(highlightEl.attr("data-surface")).isEqualTo("highlight");
    assertThat(highlightEl.selectFirst("h3#sec-highlight--heading")).isNotNull();
    assertThat(highlightEl.selectFirst("h3#sec-highlight--heading").text())
        .isEqualTo("Highlight Section");
  }

  @Test
  void contentSectionLevelValidation() {
    assertThatThrownBy(() -> ContentSection.create("bad", "Bad").level(1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ContentSection.create("bad", "Bad").level(5))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void tocCollapsibleMarkupAndChevron() {
    var items =
        java.util.List.of(
            Toc.Item.h2("intro", "Introduction"),
            Toc.Item.h3("sub-intro", "Detailed Architecture"),
            Toc.Item.h2("summary", "Summary"));

    var toc = Toc.of("doc-toc", "On This Page", items);

    var doc =
        new PageDocument(
            "Toc Test",
            main()
                .children(
                    div().attr("id", "intro"),
                    div().attr("id", "sub-intro"),
                    div().attr("id", "summary"),
                    toc));
    var compiled = PageCompiler.compile(doc);
    var parsed = Jsoup.parse(compiled.html());

    var nav = parsed.selectFirst("nav.toc-container");
    assertThat(nav).isNotNull();
    assertThat(nav.attr("aria-label")).isEqualTo("On This Page");
    assertThat(nav.attr("data-view")).isEqualTo("table-of-contents");

    var details = nav.selectFirst("details.toc-collapsible-wrapper");
    assertThat(details).isNotNull();
    assertThat(details.hasAttr("open")).isTrue();

    var summary = details.selectFirst("summary.toc-summary");
    assertThat(summary).isNotNull();
    assertThat(summary.selectFirst(".toc-title").text()).isEqualTo("On This Page");
    assertThat(summary.selectFirst("svg.toc-chevron")).isNotNull();

    var links = details.select("ol > li > a");
    assertThat(links).hasSize(3);
    assertThat(links.get(0).attr("href")).isEqualTo("#intro");
    assertThat(links.get(0).attr("data-level")).isEqualTo("2");
    assertThat(links.get(1).attr("href")).isEqualTo("#sub-intro");
    assertThat(links.get(1).attr("data-level")).isEqualTo("3");

    // CSS contains scroll-target-group, target-current, and transition rules
    assertThat(compiled.cssContent()).contains("scroll-target-group:auto");
    assertThat(compiled.cssContent()).contains(":target-current");
    assertThat(compiled.cssContent()).contains("details::details-content");
    assertThat(compiled.cssContent()).contains(".toc-chevron");
  }

  @Test
  void docLayoutFullPipelineWithProseAndContentSections() {
    var s1 =
        ContentSection.ghost("getting-started", "Getting Started")
            .badge("GUIDE")
            .leadText("How to start building with Tessera.")
            .child(p().text("Narrative step-by-step instructions."));

    var s2 =
        ContentSection.card("configuration", "Configuration Options")
            .badge("REFERENCE")
            .child(p().text("Card container for configuration values."));

    var s3 =
        ContentSection.highlight("telemetry-specs", "Telemetry & Observability")
            .level(3)
            .child(p().text("Deep dive into telemetry streams."));

    var layout = Toc.docLayout("my-doc", "Table of Contents", s1, s2, s3);

    var doc = new PageDocument("Documentation Layout Test", main().child(layout));
    var compiled = PageCompiler.compile(doc);
    var parsed = Jsoup.parse(compiled.html());

    // DocLayout grid container
    var grid = parsed.selectFirst(".doc-grid");
    assertThat(grid).isNotNull();

    // Prose column containing sections
    var proseCol = grid.selectFirst(".doc-prose-col");
    assertThat(proseCol).isNotNull();

    // Sidebar rail containing TableOfContents
    var rail = grid.selectFirst(".doc-toc-rail");
    assertThat(rail).isNotNull();

    var nav = rail.selectFirst("nav.toc-container");
    assertThat(nav).isNotNull();

    var details = nav.selectFirst("details.toc-collapsible-wrapper");
    assertThat(details).isNotNull();

    // ToC links automatically registered from ContentSections with scoped IDs
    var links = details.select("ol > li > a");
    assertThat(links).hasSize(3);
    assertThat(links.get(0).attr("href")).isEqualTo("#my-doc-layout--getting-started");
    assertThat(links.get(0).text()).isEqualTo("Getting Started");
    assertThat(links.get(1).attr("href")).isEqualTo("#my-doc-layout--configuration");
    assertThat(links.get(1).text()).isEqualTo("Configuration Options");
    assertThat(links.get(2).attr("href")).isEqualTo("#my-doc-layout--telemetry-specs");
    assertThat(links.get(2).text()).isEqualTo("Telemetry & Observability");
    assertThat(links.get(2).attr("data-level")).isEqualTo("3");

    // Sections rendered in prose col with matching IDs
    assertThat(proseCol.selectFirst("section#my-doc-layout--getting-started")).isNotNull();
    assertThat(proseCol.selectFirst("section#my-doc-layout--configuration")).isNotNull();
    assertThat(proseCol.selectFirst("section#my-doc-layout--telemetry-specs")).isNotNull();

    // Check CSS container query for 2-column layout and responsive grid rows
    assertThat(compiled.cssContent()).contains("@container (inline-size >= 48rem)");
    assertThat(compiled.cssContent()).contains("grid-template-columns:minmax(0, 1fr) 15rem");
    assertThat(compiled.cssContent()).contains("grid-row:1");
    assertThat(compiled.cssContent()).contains("grid-row:2");
  }
}
