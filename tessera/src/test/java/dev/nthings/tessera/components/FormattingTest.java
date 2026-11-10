package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.css.UnderlineStyle;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Formatting")
class FormattingTest {

  static Stream<Arguments> inlineFormats() {
    return Stream.of(
        argumentSet(
            "highlight",
            Formatting.highlight("text", Tone.ACCENT),
            "mark",
            List.of(
                "background:color-mix(in oklch, var(--accent-primary) 24%, transparent)",
                "color:var(--text-primary)",
                "padding-inline:var(--space-1)",
                "border-radius:var(--radius-sm)")),
        argumentSet(
            "underline",
            Formatting.underline("text", UnderlineStyle.WAVY, Tokens.STATUS_DANGER),
            "u",
            List.of(
                "text-decoration-line:underline",
                "text-decoration-style:wavy",
                "text-decoration-color:var(--status-danger)",
                "text-decoration-thickness:0.1em",
                "text-underline-offset:0.18em")),
        argumentSet(
            "strikethrough",
            Formatting.strikethrough("text", Tokens.TEXT_MUTED),
            "s",
            List.of(
                "text-decoration-line:line-through",
                "text-decoration-color:var(--text-muted)",
                "text-decoration-thickness:0.09em")),
        argumentSet(
            "deleted",
            Formatting.deleted("text"),
            "del",
            List.of(
                "text-decoration-line:line-through",
                "text-decoration-color:var(--status-danger)",
                "background:color-mix(in oklch, var(--status-danger) 24%, transparent)",
                "padding-inline:var(--space-1)")),
        argumentSet(
            "inserted",
            Formatting.inserted("text"),
            "ins",
            List.of(
                "text-decoration-line:underline",
                "text-decoration-color:var(--status-success)",
                "background:color-mix(in oklch, var(--status-success) 24%, transparent)",
                "padding-inline:var(--space-1)")),
        argumentSet(
            "keybinding",
            Formatting.keybinding("Ctrl+K"),
            "kbd",
            List.of(
                "background:var(--surface-subtle)",
                "font-size:0.85em",
                "border:1px solid var(--border-subtle)",
                "box-shadow:0 1px 0 var(--border-subtle)")),
        argumentSet(
            "inline code",
            Formatting.inlineCode("text"),
            "code",
            List.of(
                "background:var(--surface-subtle)",
                "font-size:0.9em",
                "padding-inline:0.35rem",
                "border-radius:var(--radius-sm)")));
  }

  @ParameterizedTest
  @MethodSource("inlineFormats")
  @DisplayName("renders its semantic tag with its signature styling")
  void rendersSemanticTagAndStyling(IntoElement format, String tagName, List<String> declarations) {
    var page = renderMain(p().child(format));
    var inline = page.element("main p > *");

    assertThat(inline).hasTagName(tagName);
    assertThat(page.ruleOf(inline, "u-")).contains(declarations.toArray(String[]::new));
  }
}
