package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Callout")
class CalloutTest {

  static Stream<Arguments> calloutsByTone() {
    return Stream.of(
        argumentSet(
            "note", Callout.of(Callout.Kind.NOTE, "Important note about JDK 25."), "note", "Note"),
        argumentSet(
            "tip",
            Callout.of(Callout.Kind.TIP, "Enable bleeding-edge CSS features."),
            "tip",
            "Tip"),
        argumentSet(
            "important",
            Callout.of(Callout.Kind.IMPORTANT, "Read this first."),
            "important",
            "Important"),
        argumentSet(
            "warning",
            Callout.of(Callout.Kind.WARNING, "Do not use preview language flags."),
            "warning",
            "Warning"),
        argumentSet(
            "caution",
            Callout.of(Callout.Kind.CAUTION, "Destructive operations require confirmation."),
            "caution",
            "Caution"));
  }

  @ParameterizedTest
  @MethodSource("calloutsByTone")
  @DisplayName("carries its tone class and a default title")
  void carriesToneClassAndDefaultTitle(Callout callout, String tone, String defaultTitle) {
    var aside = renderMain(callout).element("aside[role=note]");

    assertThat(aside).hasClasses("callout--" + tone);
    assertThat(aside.selectFirst(".callout-title")).hasText(defaultTitle);
  }

  @Test
  @DisplayName("a custom title replaces the default")
  void customTitleReplacesDefault() {
    var aside =
        renderMain(
                Callout.of(Callout.Kind.CAUTION, "Destructive operations require confirmation.")
                    .title("Hazard Ahead"))
            .element("aside[role=note]");

    assertThat(aside.selectFirst(".callout-title")).hasText("Hazard Ahead");
  }
}
