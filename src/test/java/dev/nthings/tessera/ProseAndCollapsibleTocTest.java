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
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProseAndCollapsibleTocTest {

  @Test
  @DisplayName("Prose applies a custom reading measure to its canvas")
  void proseRendersWithMaxMeasureAndTypography() {
    var prose = Prose.create("prose-spec").maxMeasure(CssLength.ch(60)).children(p().text("Text"));
    var compiled = PageCompiler.compile(new PageDocument("Prose", main().child(prose)));

    assertThat(Jsoup.parse(compiled.html()).selectFirst(".prose-canvas > p")).isNotNull();
    assertThat(compiled.cssContent()).contains("max-inline-size:60ch");
  }

  @Test
  @DisplayName("ContentSection surfaces are exposed as data-surface; level() sets the heading tag")
  void contentSectionSurfacesAndStructure() {
    var doc =
        new PageDocument(
            "Sections",
            main()
                .children(
                    ContentSection.ghost("sec-ghost", "Ghost").leadText("Lead."),
                    ContentSection.card("sec-card", "Card"),
                    ContentSection.highlight("sec-highlight", "Highlight").level(3)));
    var parsed = Jsoup.parse(PageCompiler.compile(doc).html());

    assertThat(parsed.select("main > section"))
        .extracting(e -> e.attr("data-surface"))
        .containsExactly("ghost", "card", "highlight");
    var highlight = parsed.getElementById("sec-highlight");
    assertThat(highlight.select("h2")).isEmpty();
    assertThat(highlight.select("h3")).hasSize(1);
  }

  @Test
  @DisplayName("ContentSection validates heading levels between 2 and 4")
  void contentSectionValidatesHeadingLevels() {
    assertThatThrownBy(() -> ContentSection.create("bad", "Bad").level(1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ContentSection.create("bad", "Bad").level(5))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("An explicit table of contents links each item to its target with its level")
  void tableOfContentsCollapsibleDetailsAndChevron() {
    var toc =
        Toc.of(
            "doc-toc",
            "On This Page",
            List.of(Toc.Item.h2("intro", "Introduction"), Toc.Item.h3("sub", "Details")));
    var doc =
        new PageDocument(
            "Toc", main().children(div().attr("id", "intro"), div().attr("id", "sub"), toc));
    var parsed = Jsoup.parse(PageCompiler.compile(doc).html());

    var links = parsed.select("nav[aria-label=On This Page] details ol > li > a");
    assertThat(links)
        .extracting(a -> a.attr("href") + "@" + a.attr("data-level"))
        .containsExactly("#intro@2", "#sub@3");
  }

  @Test
  @DisplayName("DocLayout lists its sections in the rail, linked by their scoped ids")
  void docLayoutIntegratesProseAndSidebarRail() {
    var layout =
        Toc.docLayout(
            "my-doc",
            "Table of Contents",
            ContentSection.ghost("getting-started", "Getting Started"),
            ContentSection.highlight("telemetry", "Telemetry").level(3));
    var parsed =
        Jsoup.parse(PageCompiler.compile(new PageDocument("Docs", main().child(layout))).html());

    String first = Toc.DocLayout.sectionDomId("my-doc", "getting-started");
    String second = Toc.DocLayout.sectionDomId("my-doc", "telemetry");
    var links = parsed.select(".doc-toc-rail nav a");
    assertThat(links)
        .extracting(a -> a.attr("href") + "@" + a.attr("data-level"))
        .containsExactly("#" + first + "@2", "#" + second + "@3");
    assertThat(parsed.select(".doc-prose-col > section"))
        .extracting(e -> e.id())
        .containsExactly(first, second);
  }
}
