package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Section")
class SectionTest {

  static Stream<Arguments> sectionsBySurface() {
    return Stream.of(
        argumentSet("ghost", Section.of("sec-ghost", "Ghost").description("Lead."), "ghost"),
        argumentSet("card", Section.of("sec-card", "Card").surface(Section.Surface.CARD), "card"),
        argumentSet(
            "highlight",
            Section.of("sec-highlight", "Highlight").surface(Section.Surface.HIGHLIGHT),
            "highlight"));
  }

  @ParameterizedTest
  @MethodSource("sectionsBySurface")
  @DisplayName("exposes its surface as data-surface")
  void exposesSurfaceAsDataSurface(Section section, String surface) {
    assertThat(renderMain(section).element("main > section")).hasAttribute("data-surface", surface);
  }

  @Test
  @DisplayName("registers its heading and keeps its page-level id inside a view")
  void registersHeadingWithPageLevelId() {
    var page =
        renderMain(
            TableOfContents.of("Contents"),
            Prose.of(Section.of("install", "Install").child(p().text("Run it."))).id("docs"));

    assertThat(page.elementById("install")).hasTagName("section");
    assertThat(page.element("nav[aria-label=Contents] a")).hasAttribute("href", "#install");
  }

  @Test
  @DisplayName("rejects an id that is empty or contains whitespace")
  void rejectsInvalidIds() {
    assertThatIllegalArgumentException().isThrownBy(() -> Section.of("two words", "Bad"));
  }

  @Test
  @DisplayName("level() sets the heading tag")
  void levelSetsHeadingTag() {
    var section = renderMain(Section.of("sec-highlight", "Highlight").level(3));

    var highlight = section.elementById("sec-highlight");
    assertThat(highlight.select("h2")).isEmpty();
    assertThat(highlight.select("h3")).hasSize(1);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 5})
  @DisplayName("level() rejects values outside 2..4")
  void levelRejectsValuesOutsideTwoToFour(int level) {
    assertThatIllegalArgumentException().isThrownBy(() -> Section.of("bad", "Bad").level(level));
  }
}
