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
        argumentSet("ghost", Section.of("sec-ghost", "Ghost"), "ghost"),
        argumentSet("flat", Section.of("sec-flat", "Flat").surface(Section.Surface.FLAT), "flat"),
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
    assertThat(renderMain(section).element("main > section"))
        .hasTagName("section")
        .hasAttribute("data-surface", surface);
  }

  @Test
  @DisplayName("registers its heading at page level inside an id scope")
  void registersHeadingWithPageId() {
    var page =
        renderMain(
            TableOfContents.of("Contents"),
            Prose.of(Section.of("install", "Install").child(p().text("Run it."))).id("docs"));

    assertThat(page.elementById("install")).hasTagName("section");
    assertThat(page.element("nav[aria-label=Contents] a"))
        .hasAttribute("href", "#install")
        .hasAttribute("data-level", "2")
        .hasText("Install");
  }

  static Stream<Arguments> invalidIds() {
    return Stream.of(argumentSet("whitespace", "two words"), argumentSet("empty", ""));
  }

  @ParameterizedTest
  @MethodSource("invalidIds")
  @DisplayName("rejects an empty id or one containing whitespace")
  void rejectsInvalidIds(String id) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Section.of(id, "Bad"))
        .withMessageContaining("non-empty and contain no whitespace");
  }

  static Stream<Arguments> headingLevels() {
    return Stream.of(
        argumentSet("level 2", 2, "h2"),
        argumentSet("level 3", 3, "h3"),
        argumentSet("level 4", 4, "h4"));
  }

  @ParameterizedTest
  @MethodSource("headingLevels")
  @DisplayName("level() sets the heading tag")
  void levelSetsHeadingTag(int level, String tag) {
    var page = renderMain(Section.of("sec-level", "Highlight").level(level));
    var section = page.elementById("sec-level");

    assertThat(section.select("h2, h3, h4")).hasSize(1);
    assertThat(section.select(tag)).hasSize(1);
    assertThat(section.selectFirst(tag)).hasText("Highlight");
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 5})
  @DisplayName("level() rejects values outside 2..4")
  void rejectsOutOfRangeLevels(int level) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Section.of("bad", "Bad").level(level))
        .withMessageContaining("must be 2 to 4")
        .withMessageContaining(Integer.toString(level));
  }
}
