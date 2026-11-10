package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.img;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.catchThrowableOfType;
import static dev.nthings.tessera.support.HtmlAssertions.tuple;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.CssLength;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

    var compiled = Compiler.compile(site);

    assertThat(compiled.pages()).containsKeys("/guides/start", "/guides/café");
    assertThat(compiled.pages().get("/guides/start")).contains("href=\"" + href + "\"");
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

    var compiled = Compiler.compile(site);

    assertThat(compiled.pages()).containsKey("/guides/start.html");
    assertThat(compiled.files()).containsKey("/assets/manual v1.pdf");
    assertThat(compiled.pages().get("/guides/start.html"))
        .contains("href=\"?q=a+b#details\"")
        .contains("href=\"./../assets/manual%20v1.pdf?download=1\"");
  }

  static Stream<Arguments> missingAnchorCases() {
    return Stream.of(
        argumentSet(
            "rejects a missing encoded fragment behind a query",
            "/docs?mode=full#not%2Bhere",
            "not+here"),
        argumentSet(
            "rejects a link to a missing anchor on the target route",
            "/docs#missing-section",
            "missing-section"));
  }

  @ParameterizedTest
  @MethodSource("missingAnchorCases")
  @DisplayName("rejects links to missing anchors")
  void rejectsMissingAnchor(String href, String fragment) {
    var site =
        SiteDefinition.of(
            new SiteRoute("/", new PageDocument("Home", main().child(a(href).text("Read")))),
            new SiteRoute(
                "/docs",
                new PageDocument(
                    "Docs", main().id(ElementRef.scoped("intro")).text("Introduction"))));

    var failure = catchThrowableOfType(CompilationException.class, () -> Compiler.compile(site));

    assertThat(failure.diagnostics())
        .extracting(
            Diagnostic::rule,
            d -> d.location().route(),
            d -> d.location().page(),
            Diagnostic::message)
        .containsExactly(
            tuple(
                "missing-route-fragment",
                "/",
                "Home",
                "Route \"/\" links to missing target anchor #" + fragment + " on route \"/docs\""));
    assertThat(failure.getMessage())
        .contains("[missing-route-fragment]", "#" + fragment, "route \"/docs\"");
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
        .extracting(
            Diagnostic::rule,
            d -> d.location().route(),
            d -> d.location().page(),
            Diagnostic::message)
        .containsExactlyInAnyOrder(
            tuple(
                "missing-route-or-asset",
                "/",
                "Home",
                "Route \"/\" references unregistered internal route or asset \"/does-not-exist\""),
            tuple(
                "missing-route-fragment",
                "/",
                "Home",
                "Route \"/\" links to missing target anchor #missing-section on route \"/docs\""),
            tuple(
                "unshared-transition-key",
                "/docs",
                "Docs",
                "TransitionKey 'docs-title' declared on route '/docs' must appear on at least two"
                    + " distinct routes site-wide to enable cross-document morphing."));
    assertThat(failure.getMessage())
        .contains(
            "[missing-route-or-asset]",
            "[missing-route-fragment]",
            "[unshared-transition-key]",
            "<a id=\"jump\">",
            "Fix:");
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
            d -> d.location().page(),
            d -> d.location().tag(),
            d -> d.location().domId(),
            Diagnostic::message)
        .containsExactlyInAnyOrder(
            tuple(
                "image-dimensions",
                "/profile",
                "Profile",
                "img",
                null,
                "<img> element with src=\"/avatar.png\" must declare both 'width' and 'height'"
                    + " attributes or a CSS 'aspect-ratio' to prevent Cumulative Layout Shift"
                    + " (CLS)"),
            tuple(
                "invalid-url",
                "/settings",
                "Settings",
                "a",
                "docs",
                "Attribute href=\"/docs%ZZ\" is not a valid URI: Malformed escape pair"),
            tuple(
                "interactive-label",
                "/settings",
                "Settings",
                "button",
                "save",
                "Interactive <button> element must have text content or an aria-label /"
                    + " aria-labelledby / title attribute"));
    assertThat(failure.getMessage())
        .contains(
            "[image-dimensions]",
            "[invalid-url]",
            "[interactive-label]",
            "Route \"/profile\"",
            "Route \"/settings\"");
  }
}
