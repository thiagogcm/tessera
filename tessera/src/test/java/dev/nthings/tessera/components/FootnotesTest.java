package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;
import static dev.nthings.tessera.support.RenderedPage.renderDocumentMain;

import dev.nthings.tessera.compiler.CompilationException;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.DocumentScope;
import dev.nthings.tessera.document.components.Footnote;
import dev.nthings.tessera.document.components.Footnotes;
import dev.nthings.tessera.support.RenderedPage;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Footnotes")
class FootnotesTest {

  /// An author scope that hosts one region of the page.
  private record Region(String id, DocumentContent content) implements DocumentScope {
    @Override
    public String scopeId() {
      return id;
    }

    @Override
    public String scopeName() {
      return "region";
    }

    @Override
    public DocumentElement expand(DocumentContext context) {
      return DocumentElement.of(div()).child(content);
    }
  }

  @Nested
  @DisplayName("references")
  class References {

    private static RenderedPage page;

    @BeforeAll
    static void renderProse() {
      page =
          renderDocumentMain(
              DocumentElement.of(p())
                  .child(Footnote.of("First footnote.").id("first").reference())
                  .text(" and ")
                  .child(Footnote.of("Second footnote.").id("second").reference()));
    }

    @Test
    @DisplayName("numbers references in document order")
    void numbersInDocumentOrder() {
      assertThat(page.elements("a[role=doc-noteref]").getFirst())
          .hasText("[1]")
          .hasAttribute("aria-label", "Footnote 1");
      assertThat(page.elements("a[role=doc-noteref]").get(1))
          .hasText("[2]")
          .hasAttribute("aria-label", "Footnote 2");
    }

    @Test
    @DisplayName("links to its note and anchors the preview tooltip")
    void linksToNoteAndTooltip() {
      var link = page.elements("a[role=doc-noteref]").getFirst();
      var tooltip = page.elementById(link.attr("interestfor"));

      assertThat(link)
          .hasAttribute("href", "#footnotes--fn-first")
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
          .containsExactly("footnotes--fn-first", "footnotes--fn-second");
    }

    @Test
    @DisplayName("each note links back to its reference")
    void notesLinkBackToReference() {
      var firstNote = page.elementById("footnotes--fn-first");

      assertThat(firstNote.selectFirst("a[role=doc-backlink]"))
          .hasAttribute("href", "#" + page.elements("a[role=doc-noteref]").getFirst().parent().id())
          .hasAttribute("aria-label", "Back to reference 1")
          .hasText("↩");
    }
  }

  @Test
  @DisplayName("an explicit section placed before references lists each note once")
  void explicitSectionListsOnce() {
    var page =
        renderDocumentMain(
            Footnotes.of().title("Notes"),
            DocumentElement.of(p()).child(Footnote.of("First note").reference()),
            DocumentElement.of(p()).child(Footnote.of("Second note").reference()));

    assertThat(page.elements("aside[role=doc-endnotes]")).hasSize(1);
    assertThat(page.elements("aside[role=doc-endnotes] li"))
        .extracting(e -> e.id())
        .containsExactly("footnotes--fn-1", "footnotes--fn-2");
    assertThat(page.element("main > :first-child")).hasAttribute("role", "doc-endnotes");
    assertThat(page.element("aside[role=doc-endnotes] h2")).hasText("Notes");
    assertThat(page.elements("a[role=doc-noteref]").get(1)).hasText("[2]");
  }

  @Test
  @DisplayName("two scopes share one endnote, and the id-bearing body is emitted once")
  void sharesOneEndnoteAcrossScopes() {
    var note =
        Footnote.of(Elements.p().id(ElementRef.page("cited")).text("Cited once."))
            .preview("Short form");
    var page =
        renderDocumentMain(
            Footnotes.of(),
            new Region("left", DocumentElement.of(p()).child(note.reference())),
            new Region("right", DocumentElement.of(p()).child(note.reference())));

    assertThat(page.elements("a[role=doc-noteref]"))
        .extracting(a -> a.text())
        .containsExactly("[1]", "[1]");
    var left = page.element("[data-scope-id=left] a[role=doc-noteref]");
    var right = page.element("[data-scope-id=right] a[role=doc-noteref]");
    assertThat(left).hasAttribute("href", "#footnotes--fn-1");
    assertThat(right).hasAttribute("href", "#footnotes--fn-1");
    assertThat(page.elements("li[role=doc-footnote]")).hasSize(1);
    assertThat(page.elementById("footnotes--fn-1").select("a[role=doc-backlink]"))
        .extracting(a -> a.attr("href") + " " + a.attr("aria-label"))
        .containsExactly(
            "#" + left.parent().id() + " Back to reference 1",
            "#" + right.parent().id() + " Back to reference 1, occurrence 2");
    assertThat(page.elements("#cited")).hasSize(1);
    assertThat(page.elements("[role=tooltip] #cited")).isEmpty();
    assertThat(page.elements("[role=tooltip]"))
        .extracting(tip -> tip.text())
        .containsExactly("Short form", "Short form");
  }

  @Test
  @DisplayName("numbers by first reference without colliding with another note's suffixed id")
  void numbersByFirstReference() {
    var later = Footnote.of("Same").id("term-2");
    var earlier = Footnote.of("Same").id("term");
    var page =
        renderDocumentMain(
            DocumentElement.of(p())
                .children(earlier.reference(), earlier.reference(), later.reference()));

    assertThat(page.elements("a[role=doc-noteref]"))
        .extracting(link -> link.text())
        .containsExactly("[1]", "[1]", "[2]");
    assertThat(page.elements("a[role=doc-backlink]"))
        .extracting(link -> link.attr("href"))
        .doesNotHaveDuplicates()
        .hasSize(3);
    assertThat(page.elements("li[role=doc-footnote]")).hasSize(2);
  }

  @Test
  @DisplayName("compiling the same definition again starts numbering over")
  void numbersOncePerCompilation() {
    var note = Footnote.of("Again");
    var first = renderDocumentMain(note.reference(), note.reference());
    var second = renderDocumentMain(note.reference(), note.reference());

    for (var page : List.of(first, second)) {
      assertThat(page.elements("a[role=doc-noteref]"))
          .extracting(a -> a.text())
          .containsExactly("[1]", "[1]");
      assertThat(page.elements("li[role=doc-footnote]")).hasSize(1);
      assertThat(page.elements("a[role=doc-backlink]")).hasSize(2);
    }
  }

  @Test
  @DisplayName("rejects reusing a footnote id on a page")
  void rejectsDuplicateIds() {
    assertThatExceptionOfType(CompilationException.class)
        .isThrownBy(() ->
            renderDocumentMain(
                Footnote.of("One").id("n").reference(), Footnote.of("Two").id("n").reference()))
        .satisfies(failure -> {
          assertThat(failure.diagnostics())
              .singleElement()
              .satisfies(d -> {
                assertThat(d.rule()).isEqualTo("duplicate-id");
                assertThat(d.location().domId()).isEqualTo("footnotes--fn-n");
              });
        });
  }
}
