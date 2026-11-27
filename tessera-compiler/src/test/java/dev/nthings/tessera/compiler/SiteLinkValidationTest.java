package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.el;
import static dev.nthings.tessera.core.Elements.img;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatNoException;
import static dev.nthings.tessera.support.HtmlAssertions.tuple;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.document.DocumentComponent;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.support.Diagnostics;
import dev.nthings.tessera.support.Diagnostics.Found;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Site link validation")
class SiteLinkValidationTest {

  private static final String[] PATHS = {"/", "/setup", "/guide", "/guides/start", "/guides/café"};

  /// A docs site whose route at `source` also holds `link`: `/guide` has an `install` anchor and
  /// `/guides/café` a `café+intro` anchor, and one PDF is registered.
  private static SiteDefinition site(String source, IntoElement link) {
    var routes = new ArrayList<SiteRoute>();
    for (var path : PATHS) {
      var body = main();
      if (path.equals("/guide")) {
        body.id(ElementRef.page("install"));
      } else if (path.equals("/guides/café")) {
        body.id(ElementRef.page("café+intro"));
      }
      if (path.equals(source)) {
        body.child(link);
      }
      routes.add(SiteRoute.of(path, PageDocument.of(path, body)));
    }
    return SiteDefinition.of(routes)
        .withBaseUrl(URI.create("https://example.com"))
        .withAssets(List.of(Asset.of("/assets/manual v1.pdf", new byte[] {1})));
  }

  static Stream<Arguments> resolvableLinks() {
    return Stream.of(
        argumentSet("an absolute route", "/", "/guide"),
        argumentSet("an index.html alias with an anchor", "/", "/guide/index.html#install"),
        argumentSet(
            "an encoded directory URL",
            "/guides/start",
            "/guides/caf%C3%A9/?mode=full#caf%C3%A9%2Bintro"),
        argumentSet(
            "an encoded sibling", "/guides/start", "../caf%C3%A9?mode=full#caf%C3%A9%2Bintro"),
        argumentSet("a query on the current page", "/guides/start", "?q=a+b"),
        argumentSet(
            "an encoded asset with a query",
            "/guides/start",
            "../../assets/manual%20v1.pdf?download=1"),
        argumentSet("an asset with a fragment", "/", "/assets/manual%20v1.pdf#page=2"),
        argumentSet("the generated sitemap", "/", "/sitemap.xml"),
        argumentSet("the generated search index", "/guide", "../search-index.json"),
        argumentSet("the top of another page", "/", "/guide#top"),
        argumentSet("the top of another page in capitals", "/", "/guides/caf%C3%A9/#TOP"),
        argumentSet("a relative path climbing above the root", "/guide", "../../setup"));
  }

  @ParameterizedTest
  @MethodSource("resolvableLinks")
  @DisplayName("links that resolve to a published page, anchor or file compile")
  void acceptsResolvableLinks(String source, String href) {
    var site = site(source, a(href).text("Read"));

    assertThatNoException().isThrownBy(() -> Compiler.forSite().compile(site));
  }

  static Stream<Arguments> brokenLinks() {
    return Stream.of(
        argumentSet(
            "an unknown route",
            "/",
            a("/does-not-exist").text("Read"),
            "missing-route-or-asset",
            "a"),
        argumentSet(
            "a path relative to the page's directory URL",
            "/guides/start",
            a("setup").text("Read"),
            "missing-route-or-asset",
            "a"),
        argumentSet(
            "a missing anchor",
            "/",
            a("/guide#missing").text("Read"),
            "missing-route-fragment",
            "a"),
        argumentSet(
            "an encoded missing anchor behind a query",
            "/",
            a("/guide?mode=full#not%2Bhere").text("Read"),
            "missing-route-fragment",
            "a"),
        argumentSet(
            "an unregistered image",
            "/guide",
            img("/assets/logo.svg", "Logo").dimensions(1, 1),
            "missing-route-or-asset",
            "img"));
  }

  @ParameterizedTest
  @MethodSource("brokenLinks")
  @DisplayName("a link that resolves to nothing is reported at its route and element")
  void reportsBrokenLinks(String source, IntoElement link, String rule, String tag) {
    var site = site(source, link);

    assertThat(Diagnostics.of(() -> Compiler.forSite().compile(site)))
        .extracting(Found::rule, Found::route, Found::tag)
        .containsExactly(tuple(rule, source, tag));
  }

  @Test
  @DisplayName("an unregistered favicon contributed to the head is reported")
  void reportsBrokenHeadLinks() {
    DocumentComponent icon =
        context -> {
          context.contributeHead(el(HtmlTag.LINK).attr("rel", "icon").attr("href", "/favicon.ico"));
          return DocumentContent.of(main().text("Home"));
        };
    var site = SiteDefinition.of(SiteRoute.of("/", PageDocument.of("Home", icon)));

    assertThat(Diagnostics.of(() -> Compiler.forSite().compile(site)))
        .extracting(Found::rule, Found::route, Found::tag)
        .containsExactly(tuple("missing-route-or-asset", "/", "link"));
  }

  @Test
  @DisplayName("broken links on several routes are all reported")
  void collectsBrokenLinksAcrossRoutes() {
    var site =
        SiteDefinition.of(
            SiteRoute.of("/", PageDocument.of("Home", main(a("/gone", "Gone")))),
            SiteRoute.of("/docs", PageDocument.of("Docs", main(a("/#missing", "Back")))));

    assertThat(Diagnostics.of(() -> Compiler.forSite().compile(site)))
        .extracting(Found::rule, Found::route)
        .containsExactlyInAnyOrder(
            tuple("missing-route-or-asset", "/"), tuple("missing-route-fragment", "/docs"));
  }
}
