package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.custom;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.img;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.catchThrowable;
import static dev.nthings.tessera.support.HtmlAssertions.tuple;
import static dev.nthings.tessera.support.SiteFixtures.docsSite;
import static dev.nthings.tessera.support.SiteFixtures.logo;
import static dev.nthings.tessera.support.SiteFixtures.stylesheetPath;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.document.DocumentComponent;
import dev.nthings.tessera.document.PageScript;
import dev.nthings.tessera.support.CssBlocks;
import dev.nthings.tessera.support.Diagnostics;
import dev.nthings.tessera.support.Diagnostics.Found;
import dev.nthings.tessera.support.Json;
import dev.nthings.tessera.support.RenderedPage;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Site compilation")
class SiteCompilationTest {

  @Test
  @DisplayName("every page links one shared stylesheet holding every route's rules")
  void sharesOneStylesheetAcrossRoutes() {
    var site =
        Compiler.forSite()
            .compile(
                SiteDefinition.of(
                    SiteRoute.of("/", PageDocument.of("Home", main().p(CssLength.rem(2)))),
                    SiteRoute.of("/about", PageDocument.of("About", main().m(CssLength.rem(3))))));
    var stylesheet = site.assets().get(stylesheetPath(site)).textContent();

    for (var route : site.pages().keySet()) {
      var page = RenderedPage.of(site, route);
      assertThat(page.elements("link[rel=stylesheet]"))
          .singleElement()
          .satisfies(link -> assertThat(link).hasAttribute("href", stylesheetPath(site)));
      assertThat(page.elements("style")).isEmpty();
      var generated = CssBlocks.generatedClass(page.element("main").className(), "u-");
      assertThat(CssBlocks.classRule(stylesheet, generated)).isNotBlank();
    }
  }

  @Test
  @DisplayName("site theme tokens are declared in the shared stylesheet")
  void declaresThemeTokens() {
    var brand = new DesignToken.Color("brand-hue", CssColor.oklch(60, 0.2, 300));
    var site =
        Compiler.forSite()
            .withTheme(Theme.defaults().with(brand, brand.defaultValue()))
            .compile(SiteDefinition.of(SiteRoute.of("/", PageDocument.of("Home", main(p("x"))))));

    assertThat(site.assets().get(stylesheetPath(site)).textContent()).contains("--brand-hue:");
  }

  private static DocumentComponent widget(PageScript script) {
    return context -> {
      context.contributeScript(script);
      return main().child(custom("test-widget"));
    };
  }

  @Test
  @DisplayName("a route's page script is published once and loaded only by that route")
  void linksPageScriptsOnlyWhereUsed() {
    var script = PageScript.of("test-widget", "customElements.define('test-widget', class {});");
    var site =
        Compiler.forSite()
            .compile(
                SiteDefinition.of(
                    SiteRoute.of("/", PageDocument.of("Home", widget(script))),
                    SiteRoute.of("/plain", PageDocument.of("Plain", main().text("Plain")))));

    var loaded = RenderedPage.of(site, "/").element("script[type=module]");
    assertThat(site.assets().get(loaded.attr("src")).textContent()).isEqualTo(script.module());
    assertThat(RenderedPage.of(site, "/plain").elements("script[type=module]")).isEmpty();
  }

  @Test
  @DisplayName("compiling twice gives identical output, and a style change renames the stylesheet")
  void outputIsDeterministicAndCacheBusted() {
    var script = PageScript.of("test-widget", "customElements.define('test-widget', class {});");
    Supplier<SiteDefinition> site =
        () ->
            SiteDefinition.of(
                SiteRoute.of("/", PageDocument.of("Home", widget(script))),
                SiteRoute.of("/about", PageDocument.of("About", main().p(CssLength.rem(1)))));
    var first = Compiler.forSite().compile(site.get());
    var second = Compiler.forSite().compile(site.get());
    var restyled =
        Compiler.forSite()
            .compile(
                SiteDefinition.of(
                    SiteRoute.of("/", PageDocument.of("Home", widget(script))),
                    SiteRoute.of("/about", PageDocument.of("About", main().p(CssLength.rem(2))))));

    assertThat(second.pages()).isEqualTo(first.pages());
    assertThat(second.assets().keySet()).isEqualTo(first.assets().keySet());
    first
        .assets()
        .forEach((path, file) ->
            assertThat(second.assets().get(path).content()).isEqualTo(file.content()));
    var home = RenderedPage.of(first, "/");
    var restyledHome = RenderedPage.of(restyled, "/");
    assertThat(restyledHome.element("link[rel=stylesheet]").attr("href"))
        .isNotEqualTo(home.element("link[rel=stylesheet]").attr("href"));
    assertThat(restyledHome.element("script[type=module]").attr("src"))
        .isEqualTo(home.element("script[type=module]").attr("src"));
  }

  /// Fails the test if any page renders: path conflicts must stop compilation first.
  private static final Component UNREACHABLE =
      _ -> {
        throw new AssertionError("A page rendered despite a path conflict");
      };

  private static SiteRoute unrendered(String path) {
    return SiteRoute.of(path, PageDocument.of(path, main(UNREACHABLE)));
  }

  static Stream<Arguments> pathConflicts() {
    return Stream.of(
        argumentSet(
            "two routes at one normalized path",
            List.of(unrendered("/a"), unrendered("a/")),
            List.of(),
            tuple("duplicate-route", "/a")),
        argumentSet(
            "an asset at a route path",
            List.of(unrendered("/"), unrendered("/guide")),
            List.of(Asset.text("/guide", "x")),
            tuple("file-path-collision", "/guide")),
        argumentSet(
            "two assets at one path",
            List.of(unrendered("/")),
            List.of(logo(), logo()),
            tuple("file-path-collision", null)),
        argumentSet(
            "an asset at a generated file path",
            List.of(unrendered("/")),
            List.of(Asset.text("/search-index.json", "[]")),
            tuple("file-path-collision", null)),
        argumentSet(
            "an asset written over the home page",
            List.of(unrendered("/")),
            List.of(Asset.text("/index.html", "<p>static</p>")),
            tuple("file-path-collision", "/")),
        argumentSet(
            "an asset written over a directory route",
            List.of(unrendered("/"), unrendered("/guide")),
            List.of(Asset.text("/guide/index.html", "<p>static</p>")),
            tuple("file-path-collision", "/guide")),
        argumentSet(
            "an .html route written over a directory route",
            List.of(unrendered("/guide"), unrendered("/guide/index.html")),
            List.of(),
            tuple("file-path-collision", "/guide/index.html")));
  }

  @ParameterizedTest
  @MethodSource("pathConflicts")
  @DisplayName("path conflicts are reported before any page renders")
  void reportsPathConflictsBeforeRendering(
      List<SiteRoute> routes, List<Asset> assets, Tuple expected) {
    var site = SiteDefinition.of(routes).withAssets(assets);

    assertThat(Diagnostics.of(() -> Compiler.forSite().compile(site)))
        .extracting(Found::rule, Found::route)
        .containsExactly(expected);
  }

  @Test
  @DisplayName("page errors from every route are reported together, before cross-route checks")
  void reportsPageErrorsBeforeCrossRouteChecks() {
    var site =
        SiteDefinition.of(
            SiteRoute.of(
                "/profile",
                PageDocument.of(
                    "Profile", main(img("/avatar.png", "Avatar"), a("/missing", "Gone")))),
            SiteRoute.of("/settings", PageDocument.of("Settings", main(button()))));

    assertThat(Diagnostics.of(() -> Compiler.forSite().compile(site)))
        .extracting(Found::rule, Found::route, Found::tag)
        .containsExactlyInAnyOrder(
            tuple("image-dimensions", "/profile", "img"),
            tuple("interactive-label", "/settings", "button"));
  }

  static Stream<Arguments> callbackFailures() {
    return Stream.of(
        argumentSet("a runtime exception", new IllegalStateException("broken")),
        argumentSet("an error", new StackOverflowError()));
  }

  @ParameterizedTest
  @MethodSource("callbackFailures")
  @DisplayName("a failing callback propagates unchanged from page and site compilation")
  void propagatesCallbackFailuresUnchanged(Throwable failure) {
    Component broken =
        _ -> {
          if (failure instanceof Error error) {
            throw error;
          }
          throw (RuntimeException) failure;
        };
    var document = PageDocument.of("Broken", main(broken));

    assertThat(catchThrowable(() -> Compiler.forPage().compile(document))).isSameAs(failure);
    assertThat(
            catchThrowable(
                () -> Compiler.forSite().compile(SiteDefinition.of(SiteRoute.of("/", document)))))
        .isSameAs(failure);
  }

  private final TransitionKey key = TransitionKey.of("page-title", "morph");

  @Test
  @DisplayName("a transition key on a single route is reported at that route")
  void rejectsUnsharedTransitionKey() {
    var site =
        SiteDefinition.of(
            SiteRoute.of("/", PageDocument.of("Home", h1("Home").transitionKey(key))),
            SiteRoute.of("/about", PageDocument.of("About", h1("About"))));

    assertThat(Diagnostics.of(() -> Compiler.forSite().compile(site)))
        .extracting(Found::rule, Found::route)
        .containsExactly(tuple("unshared-transition-key", "/"));
  }

  @Test
  @DisplayName("a transition name shared with different classes does not pair the elements")
  void rejectsTransitionKeysWithDifferentClasses() {
    var site =
        SiteDefinition.of(
            SiteRoute.of("/", PageDocument.of("Home", h1("Home").transitionKey(key))),
            SiteRoute.of(
                "/about",
                PageDocument.of(
                    "About", h1("About").transitionKey(TransitionKey.of("page-title")))));

    assertThat(Diagnostics.of(() -> Compiler.forSite().compile(site)))
        .extracting(Found::rule, Found::route)
        .containsExactlyInAnyOrder(
            tuple("unshared-transition-key", "/"), tuple("unshared-transition-key", "/about"));
  }

  @Test
  @DisplayName("a shared transition key gives both pages' elements the same transition name")
  void pairsSharedTransitionKeys() {
    var site =
        Compiler.forSite()
            .compile(
                SiteDefinition.of(
                    SiteRoute.of("/", PageDocument.of("Home", h1("Home").transitionKey(key))),
                    SiteRoute.of(
                        "/about", PageDocument.of("About", h1("About").transitionKey(key)))));

    for (var route : List.of("/", "/about")) {
      var page = RenderedPage.of(site, route);
      assertThat(page.ruleOf(page.element("h1"), "u-"))
          .contains("view-transition-name:page-title")
          .contains("view-transition-class:morph");
    }
  }

  @ParameterizedTest
  @EnumSource(LinkSpeculation.class)
  @DisplayName("pages declare same-site speculation rules for the chosen action")
  void declaresSpeculationRules(LinkSpeculation speculation) {
    var site = Compiler.forSite().compile(docsSite().withLinkSpeculation(speculation));
    var rules = RenderedPage.of(site, "/").elements("script[type=speculationrules]");

    if (speculation == LinkSpeculation.NONE) {
      assertThat(rules).isEmpty();
      return;
    }
    var declared = Json.parse(rules.getFirst().data());
    assertThat(rules).hasSize(1);
    assertThat(declared.keys()).containsExactly(speculation.name().toLowerCase(Locale.ROOT));
    var rule = declared.get(declared.keys().getFirst()).list().getFirst();
    assertThat(rule.get("where").get("href_matches").string()).isEqualTo("/*");
    assertThat(rule.get("eagerness").string()).isEqualTo("moderate");
  }

  @Test
  @DisplayName("a registered asset is published with its bytes and inferred content type")
  void publishesRegisteredAsset() {
    var file = Compiler.forSite().compile(docsSite()).assets().get(logo().path());

    assertThat(file.contentType()).isEqualTo("image/png");
    assertThat(file.content()).isEqualTo(logo().content());
  }

  @Test
  @DisplayName("an image of an unregistered asset is reported at its route")
  void rejectsUnregisteredImageAsset() {
    var site =
        SiteDefinition.of(
            SiteRoute.of("/", PageDocument.of("Home", main(img("/x.png", "X").dimensions(1, 1)))));

    assertThat(Diagnostics.of(() -> Compiler.forSite().compile(site)))
        .extracting(Found::rule, Found::route, Found::tag)
        .containsExactly(tuple("missing-route-or-asset", "/", "img"));
  }
}
