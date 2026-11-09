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
        argumentSet("highlight", Formatting.highlight("text", Tone.ACCENT), "mark"),
        argumentSet(
            "underline",
            Formatting.underline("text", UnderlineStyle.WAVY, Tokens.STATUS_DANGER),
            "u"),
        argumentSet("strikethrough", Formatting.strikethrough("text", Tokens.TEXT_MUTED), "s"),
        argumentSet("deleted", Formatting.deleted("text"), "del"),
        argumentSet("inserted", Formatting.inserted("text"), "ins"),
        argumentSet("keybinding", Formatting.keybinding("Ctrl+K"), "kbd"));
  }

  @ParameterizedTest
  @MethodSource("inlineFormats")
  @DisplayName("maps to a semantic inline tag")
  void mapsToSemanticInlineTag(IntoElement format, String tagName) {
    var inline = renderMain(p().child(format)).element("main p > *");

    assertThat(inline).hasTagName(tagName);
  }
}
