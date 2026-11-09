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
                      Footnotes.ref("fn-1", "First footnote."),
                      text(" and "),
                      Footnotes.ref("fn-2", "Second footnote.")));
    }

    @Test
    @DisplayName("are numbered in document order")
    void areNumberedInDocumentOrder() {
      assertThat(page.elementById("fnref-fn-1").selectFirst("a[role=doc-noteref]")).hasText("[1]");
      assertThat(page.elementById("fnref-fn-2").selectFirst("a")).hasText("[2]");
    }

    @Test
    @DisplayName("link to their note and anchor a preview tooltip")
    void linkToTheirNoteAndAnchorPreviewTooltip() {
      var link = page.elementById("fnref-fn-1").selectFirst("a[role=doc-noteref]");

      assertThat(link).hasAttribute("href", "#footnotes--fn-1");
      assertThat(link).hasAttribute("interestfor", link.nextElementSibling().id());
    }

    @Test
    @DisplayName("collect their notes after all content when no section is declared")
    void collectNotesAfterAllContentWithoutExplicitSection() {
      var endnotes = page.dom().body().children().last();

      assertThat(endnotes).hasAttribute("role", "doc-endnotes");
      assertThat(endnotes.select("ol > li[role=doc-footnote]"))
          .extracting(e -> e.id())
          .containsExactly("footnotes--fn-1", "footnotes--fn-2");
    }

    @Test
    @DisplayName("are linked back from their note")
    void areLinkedBackFromTheirNote() {
      var firstNote = page.elementById("footnotes--fn-1");

      assertThat(firstNote.selectFirst("a[role=doc-backlink]")).hasAttribute("href", "#fnref-fn-1");
    }
  }

  @Test
  @DisplayName("an explicit section placed before the references lists them once")
  void explicitSectionBeforeReferencesListsThemOnce() {
    var page =
        renderMain(
            Footnotes.section("Notes"),
            p().child(Footnotes.ref("a", "First note")),
            p().child(Footnotes.ref("b", "Second note")));

    assertThat(page.elements("aside[role=doc-endnotes]")).hasSize(1);
    assertThat(page.elements("aside[role=doc-endnotes] li")).hasSize(2);
    assertThat(page.element("main > :first-child")).hasAttribute("role", "doc-endnotes");
  }

  @Test
  @DisplayName("reusing a footnote id on one page is rejected")
  void rejectsDuplicateFootnoteIds() {
    assertThatExceptionOfType(CompilationException.class)
        .isThrownBy(() -> renderMain(Footnotes.ref("n", "One"), Footnotes.ref("n", "Two")))
        .withMessageContaining("duplicate-id");
  }
}
