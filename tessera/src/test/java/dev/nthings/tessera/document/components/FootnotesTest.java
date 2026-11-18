package dev.nthings.tessera.document.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderDocumentMain;

import dev.nthings.tessera.components.Section;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.DocumentScope;
import dev.nthings.tessera.support.Diagnostics;
import dev.nthings.tessera.support.RenderedPage;
import java.util.List;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Footnotes")
class FootnotesTest {

  /// An author scope that hosts one region of the page.
  private record Region(String scopeId, DocumentContent content) implements DocumentScope {
    @Override
    public String scopeName() {
      return "region";
    }

    @Override
    public DocumentElement expand(DocumentContext context) {
      return DocumentElement.of(div()).child(content);
    }
  }

  private static List<Element> references(RenderedPage page) {
    return page.elements("a[role=doc-noteref]");
  }

  /// The element a same-page `#fragment` link leads to.
  private static Element destination(RenderedPage page, Element link) {
    assertThat(link.attr("href")).startsWith("#");
    return page.elementById(link.attr("href").substring(1));
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
                  .text("Compiled")
                  .child(Footnote.of("First footnote.").id("first").reference())
                  .text(" and shipped")
                  .child(Footnote.of("Second footnote.").reference()));
    }

    @Test
    @DisplayName("are numbered in document order")
    void numbersInDocumentOrder() {
      assertThat(references(page)).extracting(Element::text).containsExactly("[1]", "[2]");
    }

    @Test
    @DisplayName("lead to their note and preview it in a tooltip")
    void linksToNoteAndTooltip() {
      var first = references(page).getFirst();

      assertThat(destination(page, first)).hasAttribute("role", "doc-footnote");
      assertThat(destination(page, first).text()).startsWith("First footnote.");
      assertThat(page.target(first, "interestfor"))
          .hasAttribute("role", "tooltip")
          .hasText("First footnote.");
    }

    @Test
    @DisplayName("without a declared list, the notes follow all content in reference order")
    void appendsNotesWithoutSection() {
      var endnotes = page.dom().body().children().last();

      assertThat(endnotes).hasAttribute("role", "doc-endnotes");
      assertThat(endnotes.select("[role=doc-footnote]"))
          .extracting(Element::text)
          .satisfiesExactly(
              note -> assertThat(note).startsWith("First footnote."),
              note -> assertThat(note).startsWith("Second footnote."));
      assertThat(references(page))
          .extracting(reference -> destination(page, reference))
          .containsExactlyElementsOf(endnotes.select("[role=doc-footnote]"));
    }

    @Test
    @DisplayName("each note links back to its reference")
    void notesLinkBackToReference() {
      for (var reference : references(page)) {
        var backlink = destination(page, reference).selectFirst("a[role=doc-backlink]");
        assertThat(destination(page, backlink).select("a[role=doc-noteref]"))
            .containsExactly(reference);
      }
    }
  }

  @Test
  @DisplayName("an explicit list placed before the references lists each note once")
  void explicitSectionListsOnce() {
    var page =
        renderDocumentMain(
            Footnotes.of().title("Notes"),
            DocumentElement.of(p()).child(Footnote.of("First note").reference()),
            DocumentElement.of(p()).child(Footnote.of("Second note").reference()));

    var endnotes = page.element("main > :first-child");
    assertThat(endnotes).hasAttribute("role", "doc-endnotes");
    assertThat(page.elements("[role=doc-endnotes]")).hasSize(1);
    assertThat(endnotes.selectFirst("h2")).hasText("Notes");
    assertThat(references(page))
        .extracting(reference -> destination(page, reference).closest("[role=doc-endnotes]"))
        .containsOnly(endnotes);
    assertThat(references(page)).extracting(Element::text).containsExactly("[1]", "[2]");
  }

  @Test
  @DisplayName("a list without references renders nothing")
  void emptyListRendersNothing() {
    var page = renderDocumentMain(Footnotes.of(), DocumentContent.of(p().text("No notes here.")));

    assertThat(page.elements("[role=doc-endnotes], [role=doc-footnote]")).isEmpty();
  }

  @Test
  @DisplayName("a note referenced from a section is numbered and the section stays outlined")
  void referenceInsideDocumentSection() {
    var page =
        renderDocumentMain(
            TableOfContents.of("Contents"),
            Section.of("install", "Install")
                .withDocument(
                    DocumentElement.of(p())
                        .text("Run it")
                        .child(Footnote.of("Twice.").reference())));

    var reference = page.element("#install a[role=doc-noteref]");
    assertThat(reference).hasText("[1]");
    assertThat(destination(page, reference).text()).startsWith("Twice.");
    assertThat(page.element("nav[aria-label=Contents] a")).hasAttribute("href", "#install");
  }

  @Test
  @DisplayName("references in two scopes share one endnote that links back to each")
  void sharesOneEndnoteAcrossScopes() {
    var note =
        Footnote.of(p().id(ElementRef.page("cited")).text("Cited once.")).preview("Short form");
    var page =
        renderDocumentMain(
            Footnotes.of(),
            new Region("left", DocumentElement.of(p()).child(note.reference())),
            new Region("right", DocumentElement.of(p()).child(note.reference())));

    var refs = references(page);
    assertThat(refs).extracting(Element::text).containsExactly("[1]", "[1]");
    var endnote = destination(page, refs.getFirst());
    assertThat(destination(page, refs.getLast())).isSameAs(endnote);
    assertThat(page.elements("[role=doc-footnote]")).containsExactly(endnote);
    assertThat(endnote.select("a[role=doc-backlink]"))
        .extracting(backlink -> destination(page, backlink).selectFirst("a[role=doc-noteref]"))
        .containsExactlyElementsOf(refs);
    assertThat(page.elements("#cited")).hasSize(1);
    assertThat(refs)
        .extracting(ref -> page.target(ref, "interestfor"))
        .allSatisfy(tip -> assertThat(tip).hasText("Short form"))
        .doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("numbers notes by first reference without colliding with a suffixed id")
  void numbersByFirstReference() {
    var later = Footnote.of("Later").id("term-2");
    var earlier = Footnote.of("Earlier").id("term");
    var page =
        renderDocumentMain(
            DocumentElement.of(p())
                .children(earlier.reference(), earlier.reference(), later.reference()));

    var refs = references(page);
    assertThat(refs).extracting(Element::text).containsExactly("[1]", "[1]", "[2]");
    assertThat(refs)
        .extracting(ref -> destination(page, ref).text().split(" ")[0])
        .containsExactly("Earlier", "Earlier", "Later");
    assertThat(page.elements("a[role=doc-backlink]"))
        .extracting(backlink -> destination(page, backlink))
        .doesNotHaveDuplicates()
        .hasSize(3);
  }

  @Test
  @DisplayName("compiling the same definition again starts numbering over")
  void numbersOncePerCompilation() {
    var note = Footnote.of("Again");

    for (var page :
        List.of(
            renderDocumentMain(note.reference(), note.reference()),
            renderDocumentMain(note.reference(), note.reference()))) {
      assertThat(references(page)).extracting(Element::text).containsExactly("[1]", "[1]");
      assertThat(page.elements("[role=doc-footnote]")).hasSize(1);
      assertThat(page.elements("a[role=doc-backlink]")).hasSize(2);
    }
  }

  @Test
  @DisplayName("two definitions with one id are reported")
  void rejectsDuplicateIds() {
    assertThat(
            Diagnostics.of(() ->
                renderDocumentMain(
                    Footnote.of("One").id("n").reference(),
                    Footnote.of("Two").id("n").reference())))
        .singleElement()
        .satisfies(found -> {
          assertThat(found.rule()).isEqualTo("duplicate-id");
          assertThat(found.tag()).isEqualTo("li");
        });
  }
}
