package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.catchThrowableOfType;
import static dev.nthings.tessera.support.HtmlAssertions.tuple;

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

    var failure =
        catchThrowableOfType(
            CompilationException.class, () -> Compiler.compile(singleOccurrenceSite));

    assertThat(failure.diagnostics())
        .extracting(
            Diagnostic::rule,
            d -> d.location().route(),
            d -> d.location().page(),
            Diagnostic::message)
        .containsExactly(
            tuple(
                "unshared-transition-key",
                "/",
                "Home",
                "TransitionKey 'page-title-morph' declared on route '/' must appear on at least"
                    + " two distinct routes site-wide to enable cross-document morphing."));
    assertThat(failure.getMessage())
        .contains("[unshared-transition-key]", "Route \"/\", page \"Home\"", "Fix:");
  }

  @Test
  @DisplayName("generates a class-specific view-transition rule for a shared key")
  void generatesClassRuleWhenShared() {
    var validSite =
        SiteDefinition.of(
            new SiteRoute("/", new PageDocument("Home", h1().text("Home").transitionKey(key))),
            new SiteRoute(
                "/about", new PageDocument("About", h1().text("About").transitionKey(key))));

    var css = Compiler.compile(validSite).css();

    assertThat(css)
        .contains("view-transition-name:page-title-morph")
        .contains("view-transition-class:morph-class")
        .contains(
            "::view-transition-group(*.morph-class){animation-duration:0.35s;"
                + "animation-timing-function:cubic-bezier(0.2,0,0,1)}");
  }
}
