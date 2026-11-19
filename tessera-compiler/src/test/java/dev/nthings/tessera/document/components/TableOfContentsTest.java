package dev.nthings.tessera.document.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderDocumentMain;

import dev.nthings.tessera.components.Section;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.OutlineHeading;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.support.Diagnostics;
import dev.nthings.tessera.support.RenderedPage;
import java.util.List;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TableOfContents")
class TableOfContentsTest {

  /// Each outline link as `level text -> destination text`.
  private static List<String> outline(RenderedPage page, Element nav) {
    return nav.select("a").stream()
        .map(link ->
            link.attr("data-level")
                + " "
                + link.text()
                + " -> "
                + page.elementById(link.attr("href").substring(1)).text())
        .toList();
  }

  @Test
  @DisplayName("explicit headings link to their targets with their levels")
  void linksItemsWithLevels() {
    var intro = ElementRef.page("intro");
    var sub = ElementRef.scoped("sub");
    var toc =
        TableOfContents.of("On This Page")
            .headings(
                List.of(
                    new OutlineHeading(intro, "Introduction", 2),
                    new OutlineHeading(sub, "Details", 3)));

    var page =
        renderDocumentMain(
            DocumentContent.of(div().id(intro).text("Intro body")),
            DocumentContent.of(div().id(sub).text("Detail body")),
            toc);

    assertThat(outline(page, page.element("nav[aria-label=On This Page]")))
        .containsExactly("2 Introduction -> Intro body", "3 Details -> Detail body");
  }

  @Test
  @DisplayName("a heading whose target is never bound is reported on its link")
  void rejectsUnboundTargets() {
    var brokenToc =
        TableOfContents.of("Contents")
            .headings(
                List.of(new OutlineHeading(ElementRef.page("missing-section"), "Missing", 2)));

    assertThat(Diagnostics.of(() -> renderDocumentMain(brokenToc)))
        .singleElement()
        .satisfies(found -> {
          assertThat(found.rule()).isEqualTo("unbound-reference");
          assertThat(found.tag()).isEqualTo("a");
          assertThat(found.components()).contains("TableOfContents");
        });
  }

  @Test
  @DisplayName("without explicit headings, it lists the sections placed after it")
  void listsFollowingSections() {
    var page =
        renderDocumentMain(
            TableOfContents.of("On this page"),
            DocumentContent.of(Section.of("intro", "Introduction").child(p().text("a"))),
            DocumentContent.of(Section.of("usage", "Usage").level(3).child(p().text("b"))));

    assertThat(outline(page, page.element("nav[aria-label=On this page]")))
        .containsExactly("2 Introduction -> Introduction a", "3 Usage -> Usage b");
  }

  @Test
  @DisplayName("a doc layout lists its sections in a named rail")
  void listsSectionsInRail() {
    var layout =
        DocLayout.of(
                Section.of("getting-started", "Getting Started").child(p().text("a")),
                Section.of("telemetry", "Telemetry").level(3).child(p().text("b")))
            .tocTitle("Table of Contents");

    var page = renderDocumentMain(layout);

    assertThat(outline(page, page.element("nav[aria-label=Table of Contents]")))
        .containsExactly("2 Getting Started -> Getting Started a", "3 Telemetry -> Telemetry b");
  }
}
