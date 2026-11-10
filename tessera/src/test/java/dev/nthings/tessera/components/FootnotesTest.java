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
      assertThat(page.elementById("fnref-fn-1").selectFirst("a[role=doc-noteref]")).hasText("[1]");
      assertThat(page.elementById("fnref-fn-2").selectFirst("a")).hasText("[2]");
    }

    @Test
    @DisplayName("links to its note and anchors the preview tooltip")
    void linksToNoteAndTooltip() {
      var link = page.elementById("fnref-fn-1").selectFirst("a[role=doc-noteref]");

      assertThat(link).hasAttribute("href", "#footnotes--fn-1");
      assertThat(link).hasAttribute("interestfor", link.nextElementSibling().id());
    }

    @Test
    @DisplayName("appends endnotes after all content when no section is declared")
    void appendsNotesWithoutSection() {
      var endnotes = page.dom().body().children().last();

      assertThat(endnotes).hasAttribute("role", "doc-endnotes");
      assertThat(endnotes.select("ol > li[role=doc-footnote]"))
          .extracting(e -> e.id())
          .containsExactly("footnotes--fn-1", "footnotes--fn-2");
    }

    @Test
    @DisplayName("each note links back to its reference")
    void notesLinkBackToReference() {
      var firstNote = page.elementById("footnotes--fn-1");

      assertThat(firstNote.selectFirst("a[role=doc-backlink]")).hasAttribute("href", "#fnref-fn-1");
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
    assertThat(page.elements("aside[role=doc-endnotes] li")).hasSize(2);
    assertThat(page.element("main > :first-child")).hasAttribute("role", "doc-endnotes");
    assertThat(page.element("aside[role=doc-endnotes] h2")).hasText("Notes");
    assertThat(page.elementById("fnref-2").selectFirst("a")).hasText("[2]");
  }

  @Test
  @DisplayName("rejects reusing a footnote id on a page")
  void rejectsDuplicateIds() {
    assertThatExceptionOfType(CompilationException.class)
        .isThrownBy(() -> renderMain(Footnote.of("One").id("n"), Footnote.of("Two").id("n")))
        .withMessageContaining("duplicate-id");
  }
}
