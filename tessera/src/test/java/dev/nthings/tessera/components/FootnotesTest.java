package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.text;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.compiler.CompilationException;
import dev.nthings.tessera.support.RenderedPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Footnotes")
class FootnotesTest {

  @Nested
  @DisplayName("references")
  class References {

    private static RenderedPage page;

    @BeforeAll
    static void renderProse() {
      page =
          renderMain(
              p().children(
                      Footnote.of("First footnote.").id("fn-1"),
                      text(" and "),
                      Footnote.of("Second footnote.").id("fn-2")));
    }

    @Test
    @DisplayName("numbers references in document order")
    void numbersInDocumentOrder() {
      assertThat(page.elementById("fnref-fn-1").selectFirst("a[role=doc-noteref]"))
          .hasText("[1]")
          .hasAttribute("aria-label", "Footnote 1");
      assertThat(page.elementById("fnref-fn-2").selectFirst("a"))
          .hasText("[2]")
          .hasAttribute("aria-label", "Footnote 2");
    }

    @Test
    @DisplayName("links to its note and anchors the preview tooltip")
    void linksToNoteAndTooltip() {
      var link = page.elementById("fnref-fn-1").selectFirst("a[role=doc-noteref]");
      var tooltip = page.elementById(link.attr("interestfor"));

      assertThat(link)
          .hasAttribute("href", "#footnotes--fn-1")
          .hasAttribute("interestfor", link.nextElementSibling().id());
      assertThat(tooltip)
          .hasAttribute("popover", "hint")
          .hasAttribute("role", "tooltip")
          .hasText("First footnote.");
    }

    @Test
    @DisplayName("appends endnotes after all content when no section is declared")
    void appendsNotesWithoutSection() {
      var endnotes = page.dom().body().children().last();

      assertThat(endnotes).hasAttribute("role", "doc-endnotes");
      assertThat(endnotes.selectFirst("h2")).hasText("Footnotes");
      assertThat(endnotes.select("ol > li[role=doc-footnote]"))
          .extracting(e -> e.id())
          .containsExactly("footnotes--fn-1", "footnotes--fn-2");
    }

    @Test
    @DisplayName("each note links back to its reference")
    void notesLinkBackToReference() {
      var firstNote = page.elementById("footnotes--fn-1");

      assertThat(firstNote.selectFirst("a[role=doc-backlink]"))
          .hasAttribute("href", "#fnref-fn-1")
          .hasAttribute("aria-label", "Back to reference 1")
          .hasText("↩");
    }
  }

  @Test
  @DisplayName("an explicit section placed before references lists each note once")
  void explicitSectionListsOnce() {
    var page =
        renderMain(
            Footnotes.of().title("Notes"),
            p().child(Footnote.of("First note")),
            p().child(Footnote.of("Second note")));

    assertThat(page.elements("aside[role=doc-endnotes]")).hasSize(1);
    assertThat(page.elements("aside[role=doc-endnotes] li"))
        .extracting(e -> e.id())
        .containsExactly("footnotes--fn-1", "footnotes--fn-2");
    assertThat(page.element("main > :first-child")).hasAttribute("role", "doc-endnotes");
    assertThat(page.element("aside[role=doc-endnotes] h2")).hasText("Notes");
    assertThat(page.elementById("fnref-2").selectFirst("a")).hasText("[2]");
  }

  @Test
  @DisplayName("rejects reusing a footnote id on a page")
  void rejectsDuplicateIds() {
    assertThatExceptionOfType(CompilationException.class)
        .isThrownBy(() -> renderMain(Footnote.of("One").id("n"), Footnote.of("Two").id("n")))
        .satisfies(
            failure ->
                assertThat(failure.diagnostics())
                    .isNotEmpty()
                    .allSatisfy(
                        d -> {
                          assertThat(d.rule()).isEqualTo("duplicate-id");
                          assertThat(d.message()).contains("Duplicate DOM id");
                          assertThat(d.fix()).contains("distinct ID");
                          assertThat(d.location().components()).containsExactly("Footnote");
                        })
                    .anySatisfy(d -> assertThat(d.message()).contains("'fnref-n'"))
                    .anySatisfy(d -> assertThat(d.message()).contains("'fnlink-n--tooltip'")));
  }
}
