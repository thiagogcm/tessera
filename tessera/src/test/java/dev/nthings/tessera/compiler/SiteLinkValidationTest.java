package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.img;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatCode;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatExceptionOfType;
import static dev.nthings.tessera.support.HtmlAssertions.catchThrowableOfType;
import static dev.nthings.tessera.support.HtmlAssertions.tuple;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.CssLength;
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
  @DisplayName("accepts relative and encoded links to published directory URLs")
  void acceptsEncodedDirectoryLinks(String href) {
    var site =
        SiteDefinition.of(
            new SiteRoute(
                "/guides/start", new PageDocument("Start", main().child(a(href).text("Read")))),
            new SiteRoute(
                "/guides/café",
                new PageDocument("Café", main().id(ElementRef.page("café+intro")))));

    assertThatCode(() -> Compiler.compile(site)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("accepts query-only links and relative asset URLs")
  void acceptsQueryOnlyLinksAndAssets() {
    var site =
        SiteDefinition.builder()
            .route(
                new SiteRoute(
                    "/guides/start.html",
                    new PageDocument(
                        "Start",
                        main()
                            .id(ElementRef.page("details"))
                            .children(
                                a("?q=a+b#details").text("Filter"),
                                a("./../assets/manual%20v1.pdf?download=1").text("Download")))))
            .asset(Asset.of("/assets/manual v1.pdf", new byte[] {1}))
            .build();

    assertThatCode(() -> Compiler.compile(site)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("rejects a missing encoded fragment behind a query")
  void rejectsEncodedMissingFragment() {
    var site =
        SiteDefinition.of(
            new SiteRoute(
                "/",
                new PageDocument(
                    "Home", main().child(a("/docs?mode=full#not%2Bhere").text("Read")))),
            new SiteRoute("/docs", new PageDocument("Docs", main())));

    assertThatExceptionOfType(CompilationException.class)
        .isThrownBy(() -> Compiler.compile(site))
        .withMessageContaining("#not+here");
  }

  @Test
  @DisplayName("collects link and transition errors across routes")
  void collectsCrossRouteErrors() {
    var site =
        SiteDefinition.of(
            new SiteRoute(
                "/",
                new PageDocument(
                    "Home",
                    main()
                        .children(
                            a("/does-not-exist").px(CssLength.rem(1)).text("Broken link"),
                            a("/docs#missing-section")
                                .id(ElementRef.scoped("jump"))
                                .text("Jump to section")))),
            new SiteRoute(
                "/docs",
                new PageDocument("Docs", main().transitionKey(TransitionKey.of("docs-title")))));

    var failure = catchThrowableOfType(CompilationException.class, () -> Compiler.compile(site));

    assertThat(failure.diagnostics())
        .extracting(Diagnostic::rule, d -> d.location().route(), d -> d.location().page())
        .containsExactlyInAnyOrder(
            tuple("missing-route-or-asset", "/", "Home"),
            tuple("missing-route-fragment", "/", "Home"),
            tuple("unshared-transition-key", "/docs", "Docs"));
    assertThat(failure.getMessage())
        .contains("/does-not-exist", "missing-section", "<a id=\"jump\">", "Fix:");
  }

  @Test
  @DisplayName("collects page errors across routes")
  void collectsPageErrors() {
    var site =
        SiteDefinition.of(
            new SiteRoute("/profile", new PageDocument("Profile", img("/avatar.png", "Avatar"))),
            new SiteRoute(
                "/settings",
                new PageDocument(
                    "Settings",
                    main()
                        .children(
                            a("/docs%ZZ").id(ElementRef.scoped("docs")).text("Documentation"),
                            button().id(ElementRef.scoped("save"))))));

    var failure = catchThrowableOfType(CompilationException.class, () -> Compiler.compile(site));

    assertThat(failure.diagnostics())
        .extracting(
            Diagnostic::rule,
            d -> d.location().route(),
            d -> d.location().tag(),
            d -> d.location().domId())
        .containsExactlyInAnyOrder(
            tuple("image-dimensions", "/profile", "img", null),
            tuple("invalid-url", "/settings", "a", "docs"),
            tuple("interactive-label", "/settings", "button", "save"));
    assertThat(failure.getMessage()).contains("Route \"/profile\"", "Route \"/settings\"");
  }

  @Test
  @DisplayName("rejects a link to a missing anchor on the target route")
  void rejectsMissingAnchor() {
    var site =
        SiteDefinition.of(
            new SiteRoute(
                "/",
                new PageDocument(
                    "Home", main().child(a("/docs#missing-section").text("Jump to section")))),
            new SiteRoute(
                "/docs",
                new PageDocument(
                    "Docs", main().id(ElementRef.scoped("intro")).text("Introduction"))));

    assertThatExceptionOfType(CompilationException.class)
        .isThrownBy(() -> Compiler.compile(site))
        .withMessageContaining("missing target anchor #missing-section on route \"/docs\"");
  }
}
