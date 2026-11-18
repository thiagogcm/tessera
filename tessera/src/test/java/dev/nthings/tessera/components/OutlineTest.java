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
import dev.nthings.tessera.support.Diagnostics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("headings and sections")
class OutlineTest {

  @ParameterizedTest
  @ValueSource(ints = {0, 7})
  @DisplayName("a heading level must be 1 through 6")
  void headingRejectsLevelsOutsideHtml(int level) {
    assertThatIllegalArgumentException().isThrownBy(() -> Heading.of(level, "Bad"));
  }

  @Test
  @DisplayName("a heading keeps rich phrasing while the outline uses its plain label")
  void separatesVisibleTitleFromNavLabel() {
    var install = ElementRef.page("install");
    var page =
        renderDocumentMain(
            TableOfContents.of("Contents"),
            DocumentContent.of(
                Heading.of(2, "Install", code().text("mvn"), text(" verify"))
                    .id(install)
                    .outline(install)));

    var link = page.element("nav[aria-label=Contents] a");
    assertThat(link).hasText("Install");
    var heading = page.elementById(link.attr("href").substring(1));
    assertThat(heading).hasTagName("h2").hasText("mvn verify");
    assertThat(heading.selectFirst("code")).hasText("mvn");
  }

  @Test
  @DisplayName("the outline lists opted-in headings and sections, not interface headings")
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
        .extracting(a -> a.text() + " -> " + page.elementById(a.attr("href").substring(1)).text())
        .containsExactly("Real -> Real", "Section -> Section");
  }

  @Test
  @DisplayName("a section keeps its page id inside a component and is outlined under it")
  void keepsPageIdOnTheSection() {
    var page =
        renderDocumentMain(
            TableOfContents.of("Contents"),
            DocumentContent.of(
                Prose.of(Section.of("install", "Install").child(p().text("Run it."))).id("docs")));

    var link = page.element("nav[aria-label=Contents] a");
    assertThat(link).hasAttribute("href", "#install").hasText("Install");
    var section = page.elementById("install");
    assertThat(section).hasTagName("section");
    assertThat(section.selectFirst("h2")).hasText("Install");
  }

  @ParameterizedTest
  @ValueSource(strings = {"two words", ""})
  @DisplayName("a section id must be non-empty and free of whitespace")
  void sectionRejectsInvalidIds(String id) {
    assertThatIllegalArgumentException().isThrownBy(() -> Section.of(id, "Bad"));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 5})
  @DisplayName("a section heading level must be 2 through 4")
  void sectionRejectsOutOfRangeLevels(int level) {
    assertThatIllegalArgumentException().isThrownBy(() -> Section.of("bad", "Bad").level(level));
  }

  @Test
  @DisplayName("two sections with one id are reported")
  void reportsDuplicateSectionIds() {
    assertThat(
            Diagnostics.of(
                () -> renderMain(Section.of("intro", "Intro"), Section.of("intro", "Again"))))
        .isNotEmpty()
        .allSatisfy(found -> {
          assertThat(found.rule()).isEqualTo("duplicate-id");
          assertThat(found.components()).containsExactly("Section");
        });
  }
}
