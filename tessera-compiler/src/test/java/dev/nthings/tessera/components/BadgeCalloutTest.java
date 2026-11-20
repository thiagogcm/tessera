package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.support.AccessibleText;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("badges and callouts")
class BadgeCalloutTest {

  static Stream<Arguments> decoratedBadges() {
    return Stream.of(
        argumentSet("status dot", Badge.of("Online").tone(Tone.SUCCESS).dot()),
        argumentSet("icon", Badge.of("Online").icon(span().text("●"))));
  }

  @ParameterizedTest
  @MethodSource("decoratedBadges")
  @DisplayName("a badge's decoration is hidden, so it reads as its label")
  void decorationIsHidden(Badge badge) {
    var rendered = renderMain(badge).element("main > *");

    assertThat(rendered.select("[aria-hidden=true]")).isNotEmpty();
    assertThat(AccessibleText.of(rendered)).isEqualTo("Online");
  }

  @Test
  @DisplayName("a badge with an href is a link named by its label")
  void hrefRendersLink() {
    var link = renderMain(Badge.of("Docs").href("/docs")).element("main > *");

    assertThat(link).hasTagName("a").hasAttribute("href", "/docs");
    assertThat(AccessibleText.of(link)).isEqualTo("Docs");
  }

  static Stream<Arguments> callouts() {
    var body = "Deploys are frozen.";
    return Stream.of(
        argumentSet("default title", Callout.of(Callout.Kind.NOTE, body), "Note"),
        argumentSet(
            "custom title",
            Callout.of(Callout.Kind.CAUTION, body).title("Hazard Ahead"),
            "Hazard Ahead"));
  }

  @ParameterizedTest
  @MethodSource("callouts")
  @DisplayName("a callout is a note that reads as its title then its body, with a hidden icon")
  void calloutReadsTitleThenBody(Callout callout, String title) {
    var note = renderMain(callout).element("main > *");

    assertThat(note).hasAttribute("role", "note");
    assertThat(note.select("[aria-hidden=true]")).isNotEmpty();
    assertThat(AccessibleText.of(note)).isEqualTo(title + " Deploys are frozen.");
  }
}
