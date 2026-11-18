package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.code;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.text;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.RenderedPage.renderDocumentMain;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.components.Footnote;
import dev.nthings.tessera.document.components.Footnotes;
import dev.nthings.tessera.document.components.TableOfContents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Heading")
class HeadingTest {

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5, 6})
  @DisplayName("renders each native heading level")
  void rendersNativeLevels(int level) {
    var page = renderMain(Heading.of(level, "Title").id(ElementRef.page("title")));

    assertThat(page.elementById("title")).hasTagName("h" + level).hasText("Title");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 7})
  @DisplayName("rejects a level outside 1 through 6")
  void rejectsLevelsOutsideHtml(int level) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Heading.of(level, "Bad"))
        .withMessage("Heading level must be 1 to 6: " + level);
  }

  @Test
  @DisplayName("keeps rich phrasing in the heading and the plain label in the outline")
  void separatesVisibleTitleFromNavLabel() {
    var install = ElementRef.page("install");
    var page =
        renderDocumentMain(
            TableOfContents.of("Contents"),
            DocumentContent.of(
                Heading.of(2, "Install", code().text("mvn"), text(" verify"))
                    .id(install)
                    .outline(install)));

    assertThat(page.elementById("install")).hasTagName("h2").hasText("mvn verify");
    assertThat(page.element("h2 > code")).hasText("mvn");
    assertThat(page.element("nav[aria-label=Contents] a"))
        .hasAttribute("href", "#install")
        .hasAttribute("data-level", "2")
        .hasText("Install");
  }

  @Test
  @DisplayName("omits interface headings and does not list a section twice")
  void outlinesOnlyOptedInHeadings() {
    var real = ElementRef.page("real");
    var page =
        renderDocumentMain(
            TableOfContents.of("Contents"),
            DocumentContent.of(h2().text("Toolbar")),
            DocumentContent.of(Card.of("Widget").headingLevel(2)),
            Footnotes.of().title("Notes"),
            DocumentElement.of(p()).child(Footnote.of("A detail.").reference()),
            DocumentContent.of(Heading.of(2, "Real").id(real).outline(real)),
            DocumentContent.of(Section.of("sec", "Section")));

    assertThat(page.elements("nav[aria-label=Contents] a"))
        .extracting(a -> a.attr("href") + ":" + a.text())
        .containsExactly("#real:Real", "#sec:Section");
    assertThat(page.element("aside[role=doc-endnotes] h2")).hasText("Notes");
  }
}
