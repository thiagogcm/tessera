package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;

import dev.nthings.tessera.core.TransitionKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Site-wide TransitionKey validation")
class SiteTransitionKeyTest {

  private final TransitionKey key = TransitionKey.of("page-title-morph", "morph-class");

  @Test
  @DisplayName("rejects a key declared on only one route")
  void rejectsUnsharedKey() {
    var singleOccurrenceSite =
        SiteDefinition.of(
            new SiteRoute("/", new PageDocument("Home", h1().text("Home").transitionKey(key))),
            new SiteRoute("/about", new PageDocument("About", h1().text("About"))));

    assertThatExceptionOfType(CompilationException.class)
        .isThrownBy(() -> Compiler.compile(singleOccurrenceSite))
        .withMessageContainingAll(
            "TransitionKey 'page-title-morph'",
            "must appear on at least two distinct routes site-wide");
  }

  @Test
  @DisplayName("generates a class-specific view-transition rule for a shared key")
  void generatesClassRuleWhenShared() {
    var validSite =
        SiteDefinition.of(
            new SiteRoute("/", new PageDocument("Home", h1().text("Home").transitionKey(key))),
            new SiteRoute(
                "/about", new PageDocument("About", h1().text("About").transitionKey(key))));

    assertThat(Compiler.compile(validSite).css())
        .contains("::view-transition-group(*.morph-class)");
  }
}
