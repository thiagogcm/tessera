package dev.nthings.tessera.site;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatCode;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;

import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.render.AccessibilityViolationException;
import dev.nthings.tessera.render.PageDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Site link validation")
class SiteLinkValidationTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "../caf%C3%A9?mode=full#caf%C3%A9%2Bintro",
        "/guides/caf%C3%A9/?mode=full#caf%C3%A9%2Bintro",
        "../caf%C3%A9/index.html#caf%C3%A9%2Bintro"
      })
  @DisplayName("resolves relative and encoded links against published directory URLs")
  void resolvesRelativeAndEncodedLinksAgainstPublishedDirectoryUrls(String href) {
    var site =
        SiteDefinition.of(
            new SiteRoute(
                "/guides/start", new PageDocument("Start", main().child(a(href).text("Read")))),
            new SiteRoute(
                "/guides/café", new PageDocument("Café", main().attr("id", "café+intro"))));

    assertThatCode(() -> SiteCompiler.compile(site)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("handles query-only links on HTML files and relative asset URLs")
  void handlesQueryOnlyLinksOnHtmlFilesAndRelativeAssetUrls() {
    var site =
        SiteDefinition.builder()
            .route(
                new SiteRoute(
                    "/guides/start.html",
                    new PageDocument(
                        "Start",
                        main()
                            .attr("id", "details")
                            .children(
                                a("?q=a+b#details").text("Filter"),
                                a("./../assets/manual%20v1.pdf?download=1").text("Download")))))
            .asset(SiteAsset.of("/assets/manual v1.pdf", new byte[] {1}))
            .build();

    assertThatCode(() -> SiteCompiler.compile(site)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("rejects missing targets behind queries and encoding")
  void rejectsMissingTargetsBehindQueriesAndEncoding() {
    var site =
        SiteDefinition.of(
            new SiteRoute(
                "/",
                new PageDocument(
                    "Home", main().child(a("/docs?mode=full#not%2Bhere").text("Read")))),
            new SiteRoute("/docs", new PageDocument("Docs", main())));

    assertThatExceptionOfType(AccessibilityViolationException.class)
        .isThrownBy(() -> SiteCompiler.compile(site))
        .withMessageContaining("#not+here");
  }

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
