package dev.nthings.tessera.site;

import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;

import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.render.AccessibilityViolationException;
import dev.nthings.tessera.render.PageDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Site-wide TransitionKey validation")
class SiteTransitionKeyTest {

  private final TransitionKey key = TransitionKey.of("page-title-morph", "morph-class");

  @Test
  @DisplayName("a key must appear on at least two distinct routes")
  void mustAppearOnAtLeastTwoRoutes() {
    var singleOccurrenceSite =
        SiteDefinition.of(
            new SiteRoute("/", new PageDocument("Home", h1().text("Home").transitionKey(key))),
            new SiteRoute("/about", new PageDocument("About", h1().text("About"))));

    assertThatExceptionOfType(AccessibilityViolationException.class)
        .isThrownBy(() -> SiteCompiler.compile(singleOccurrenceSite))
        .withMessageContainingAll(
            "TransitionKey 'page-title-morph'",
            "must appear on at least two distinct routes site-wide");
  }

  @Test
  @DisplayName("a key shared by two routes generates a class-specific timing rule")
  void generatesClassSpecificTimingRuleWhenShared() {
    var validSite =
        SiteDefinition.of(
            new SiteRoute("/", new PageDocument("Home", h1().text("Home").transitionKey(key))),
            new SiteRoute(
                "/about", new PageDocument("About", h1().text("About").transitionKey(key))));

    assertThat(SiteCompiler.compile(validSite).sharedCssContent())
        .contains("::view-transition-group(*.morph-class)");
  }
}
