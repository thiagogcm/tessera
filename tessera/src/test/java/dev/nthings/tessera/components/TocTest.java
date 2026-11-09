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

@DisplayName("Toc")
class TocTest {

  @Nested
  @DisplayName("explicit table of contents")
  class Explicit {

    @Test
    @DisplayName("links each item to its target with its heading level")
    void linksEachItemToItsTargetWithItsLevel() {
      var intro = ElementRef.page("intro");
      var sub = ElementRef.scoped("sub");
      var toc =
          Toc.of(
              "doc-toc",
              "On This Page",
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
          Toc.of(
              "broken-toc",
              "Contents",
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
    @DisplayName("placed before its sections, lists them")
    void placedBeforeItsSectionsListsThem() {
      var page =
          renderMain(
              Toc.fromContext("toc", "On this page"),
              Toc.section("intro", "Introduction", p().text("a")),
              Toc.section("usage", "Usage", p().text("b")));

      assertThat(page.elements("nav.toc-container a"))
          .extracting(a -> a.attr("href"))
          .containsExactly("#intro", "#usage");
    }

    @Test
    @DisplayName("reusing a section id on one page is rejected")
    void rejectsDuplicateSectionIds() {
      assertThatExceptionOfType(CompilationException.class)
          .isThrownBy(
              () ->
                  renderMain(
                      Toc.section("intro", "Intro", p()), Toc.section("intro", "Again", p())))
          .withMessageContaining("duplicate-id");
    }
  }

  @Test
  @DisplayName("a doc layout lists its sections in the rail, linked by their page-level ids")
  void listsSectionsInRailLinkedByPageIds() {
    var layout =
        Toc.docLayout(
            "my-doc",
            "Table of Contents",
            ContentSection.ghost("getting-started", "Getting Started"),
            ContentSection.highlight("telemetry", "Telemetry").level(3));

    var page = renderMain(layout);

    assertThat(page.elements(".doc-toc-rail nav a"))
        .extracting(a -> a.attr("href") + "@" + a.attr("data-level"))
        .containsExactly("#getting-started@2", "#telemetry@3");
    assertThat(page.elements(".doc-prose-col > section"))
        .extracting(e -> e.id())
        .containsExactly("getting-started", "telemetry");
  }
}
