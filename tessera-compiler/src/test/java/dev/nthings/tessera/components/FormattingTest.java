package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.css.UnderlineStyle;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Formatting")
class FormattingTest {

  static Stream<Arguments> inlineFormats() {
    return Stream.of(
        argumentSet("highlight", Formatting.highlight("a < b", Tone.ACCENT), "mark"),
        argumentSet(
            "underline",
            Formatting.underline("a < b", UnderlineStyle.WAVY, Tokens.STATUS_DANGER),
            "u"),
        argumentSet("strikethrough", Formatting.strikethrough("a < b", Tokens.TEXT_MUTED), "s"),
        argumentSet("deleted", Formatting.deleted("a < b"), "del"),
        argumentSet("inserted", Formatting.inserted("a < b"), "ins"),
        argumentSet("keybinding", Formatting.keybinding("a < b"), "kbd"),
        argumentSet("inline code", Formatting.inlineCode("a < b"), "code"));
  }

  @ParameterizedTest
  @MethodSource("inlineFormats")
  @DisplayName("wraps its text in the element that carries its meaning")
  void rendersSemanticElement(IntoElement format, String tagName) {
    var inline = renderMain(p().child(format)).element("main p > *");

    assertThat(inline).hasTagName(tagName).hasText("a < b");
  }
}
