package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.compiler.CompilationException;
import dev.nthings.tessera.core.DocumentHeading;
import dev.nthings.tessera.core.ElementRef;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TableOfContents")
class TableOfContentsTest {

  @Nested
  @DisplayName("explicit table of contents")
  class Explicit {

    @Test
    @DisplayName("links each item to its target with its heading level")
    void linksItemsWithLevels() {
      var intro = ElementRef.page("intro");
      var sub = ElementRef.scoped("sub");
      var toc =
          TableOfContents.of("On This Page")
              .headings(
                  List.of(
                      new DocumentHeading(intro, "Introduction", 2),
                      new DocumentHeading(sub, "Details", 3)));

      var page = renderMain(div().id(intro), div().id(sub), toc);

      assertThat(page.elements("nav[aria-label=On This Page] details ol > li > a"))
          .extracting(a -> a.attr("href") + "@" + a.attr("data-level"))
          .containsExactly("#intro@2", "#sub@3");
    }

    @Test
    @DisplayName("rejects a heading whose target is never bound")
    void rejectsUnboundTargets() {
      var brokenToc =
          TableOfContents.of("Contents")
              .headings(
                  List.of(new DocumentHeading(ElementRef.page("missing-section"), "Missing", 2)));

      assertThatExceptionOfType(CompilationException.class)
          .isThrownBy(() -> renderMain(brokenToc))
          .withMessageContaining("unbound-reference");
    }
  }

  @Nested
  @DisplayName("context table of contents")
  class FromContext {

    @Test
    @DisplayName("lists sections registered after it")
    void listsFollowingSections() {
      var page =
          renderMain(
              TableOfContents.of("On this page"),
              Section.of("intro", "Introduction").child(p().text("a")),
              Section.of("usage", "Usage").level(3).child(p().text("b")));

      assertThat(page.elements("nav.toc-container a"))
          .extracting(a -> a.attr("href") + "@" + a.attr("data-level"))
          .containsExactly("#intro@2", "#usage@3");
    }

    @Test
    @DisplayName("rejects reusing a section id on a page")
    void rejectsDuplicateIds() {
      assertThatExceptionOfType(CompilationException.class)
          .isThrownBy(() -> renderMain(Section.of("intro", "Intro"), Section.of("intro", "Again")))
          .withMessageContaining("duplicate-id");
    }
  }

  @Test
  @DisplayName("a doc layout lists its sections in the rail by page-level id")
  void listsSectionsInRail() {
    var layout =
        DocLayout.of(
                Section.of("getting-started", "Getting Started"),
                Section.of("telemetry", "Telemetry").surface(Section.Surface.HIGHLIGHT).level(3))
            .tocTitle("Table of Contents");

    var page = renderMain(layout);

    assertThat(page.elements(".doc-toc-rail nav a"))
        .extracting(a -> a.attr("href") + "@" + a.attr("data-level"))
        .containsExactly("#getting-started@2", "#telemetry@3");
    assertThat(page.element(".doc-toc-rail nav")).hasAttribute("aria-label", "Table of Contents");
    assertThat(page.elements(".doc-prose-col > section"))
        .extracting(e -> e.id())
        .containsExactly("getting-started", "telemetry");
  }
}
