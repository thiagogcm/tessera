package dev.nthings.tessera.site;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;

import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.render.AccessibilityViolationException;
import dev.nthings.tessera.render.PageDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Site link validation")
class SiteLinkValidationTest {

  @Test
  @DisplayName("rejects a link to an unregistered route")
  void rejectsLinkToUnregisteredRoute() {
    var site =
        SiteDefinition.of(
            new SiteRoute(
                "/",
                new PageDocument(
                    "Home",
                    main().child(a("/does-not-exist").px(CssLength.rem(1)).text("Broken link")))));

    assertThatExceptionOfType(AccessibilityViolationException.class)
        .isThrownBy(() -> SiteCompiler.compile(site))
        .withMessageContaining("unregistered internal route or asset \"/does-not-exist\"");
  }

  @Test
  @DisplayName("rejects a link to a #fragment missing from the target route")
  void rejectsLinkToMissingAnchor() {
    var site =
        SiteDefinition.of(
            new SiteRoute(
                "/",
                new PageDocument(
                    "Home", main().child(a("/docs#missing-section").text("Jump to section")))),
            new SiteRoute(
                "/docs", new PageDocument("Docs", main().id("intro").text("Introduction"))));

    assertThatExceptionOfType(AccessibilityViolationException.class)
        .isThrownBy(() -> SiteCompiler.compile(site))
        .withMessageContaining("missing target anchor #missing-section on route \"/docs\"");
  }
}
