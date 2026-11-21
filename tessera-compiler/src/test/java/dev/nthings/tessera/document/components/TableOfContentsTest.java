package dev.nthings.tessera.document.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.RenderedPage.renderDocumentMain;

import dev.nthings.tessera.components.Heading;
import dev.nthings.tessera.components.Section;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.OutlineHeading;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.support.CssBlocks;
import dev.nthings.tessera.support.Diagnostics;
import dev.nthings.tessera.support.RenderedPage;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("TableOfContents")
class TableOfContentsTest {

  /// Each outline link as `level text -> destination text`.
  private static DocumentContent outlined(int level, String title) {
    return outlined(new OutlineHeading(ElementRef.page("h" + level), title, level));
  }

  private static DocumentContent outlined(OutlineHeading heading) {
    var ref = heading.target();
    return DocumentContent.of(Heading.of(heading.level(), heading.title()).id(ref).outline(ref));
  }

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

  @Test
  @DisplayName("levels 3 through 6 receive progressive indentation styles")
  void indentsHeadingLevelsThreeThroughSix() {
    var page =
        renderDocumentMain(
            TableOfContents.of("Deep Outline"),
            outlined(2, "Level 2"),
            outlined(3, "Level 3"),
            outlined(4, "Level 4"),
            outlined(5, "Level 5"),
            outlined(6, "Level 6"));

    assertThat(outline(page, page.element("nav[aria-label=Deep Outline]")))
        .containsExactly(
            "2 Level 2 -> Level 2",
            "3 Level 3 -> Level 3",
            "4 Level 4 -> Level 4",
            "5 Level 5 -> Level 5",
            "6 Level 6 -> Level 6");
    var css = page.css();
    for (var level = 3; level <= 6; level++) {
      assertThat(CssBlocks.block(css, ":scope a[data-level=\"" + level + "\"]{"))
          .contains(
              "padding-inline-start:var(--space-" + (2 * level) + ")",
              "font-size:var(--font-size-xs)");
    }
  }

  @Test
  @DisplayName("levels filters both late-collected and explicit headings to the inclusive range")
  void filtersHeadingsByLevelRange() {
    var titles = List.of("Title", "Chapter", "Section", "Subsection", "Detail", "Fine");
    var explicit = new ArrayList<OutlineHeading>();
    for (var level = 1; level <= 6; level++) {
      explicit.add(new OutlineHeading(ElementRef.page("h" + level), titles.get(level - 1), level));
    }
    var content = new ArrayList<DocumentContent>();
    content.add(TableOfContents.of("Auto Filtered").levels(2, 4));
    content.add(TableOfContents.of("Explicit Filtered").headings(explicit).levels(3, 5));
    explicit.forEach(heading -> content.add(outlined(heading)));

    var page = renderDocumentMain(content.toArray(DocumentContent[]::new));

    assertThat(outline(page, page.element("nav[aria-label=Auto Filtered]")))
        .containsExactly(
            "2 Chapter -> Chapter", "3 Section -> Section", "4 Subsection -> Subsection");
    assertThat(outline(page, page.element("nav[aria-label=Explicit Filtered]")))
        .containsExactly(
            "3 Section -> Section", "4 Subsection -> Subsection", "5 Detail -> Detail");
  }

  @ParameterizedTest
  @CsvSource({"0, 3", "2, 7", "4, 2", "0, 0", "7, 7", "-1, 6"})
  @DisplayName("levels rejects bounds outside 1 through 6 or inverted ranges")
  void rejectsInvalidLevelRanges(int min, int max) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TableOfContents.of("Contents").levels(min, max));
  }
}
