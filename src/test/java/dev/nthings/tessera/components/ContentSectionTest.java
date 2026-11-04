package dev.nthings.tessera.components;

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

@DisplayName("ContentSection")
class ContentSectionTest {

  static Stream<Arguments> sectionsBySurface() {
    return Stream.of(
        argumentSet("ghost", ContentSection.ghost("sec-ghost", "Ghost").leadText("Lead."), "ghost"),
        argumentSet("card", ContentSection.card("sec-card", "Card"), "card"),
        argumentSet(
            "highlight", ContentSection.highlight("sec-highlight", "Highlight"), "highlight"));
  }

  @ParameterizedTest
  @MethodSource("sectionsBySurface")
  @DisplayName("exposes its surface as data-surface")
  void exposesSurfaceAsDataSurface(ContentSection section, String surface) {
    assertThat(renderMain(section).element("main > section")).hasAttribute("data-surface", surface);
  }

  @Test
  @DisplayName("level() sets the heading tag")
  void levelSetsHeadingTag() {
    var section = renderMain(ContentSection.highlight("sec-highlight", "Highlight").level(3));

    var highlight = section.elementById("sec-highlight");
    assertThat(highlight.select("h2")).isEmpty();
    assertThat(highlight.select("h3")).hasSize(1);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 5})
  @DisplayName("level() rejects values outside 2..4")
  void levelRejectsValuesOutsideTwoToFour(int level) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ContentSection.create("bad", "Bad").level(level));
  }
}
