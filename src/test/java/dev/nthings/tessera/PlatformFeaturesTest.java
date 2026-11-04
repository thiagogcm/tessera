package dev.nthings.tessera;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.img;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.nthings.tessera.components.CardGrid;
import dev.nthings.tessera.components.Carousel;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.ScrollReveal;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.render.AccessibilityViolationException;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PageDocument;
import dev.nthings.tessera.site.LinkSpeculation;
import dev.nthings.tessera.site.SiteAsset;
import dev.nthings.tessera.site.SiteCompiler;
import dev.nthings.tessera.site.SiteDefinition;
import dev.nthings.tessera.site.SitePreviewServer;
import dev.nthings.tessera.site.SiteRoute;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlatformFeaturesTest {

  @Test
  @DisplayName("Carousel emits native ::scroll-button prev/next controls with alt text")
  void carouselScrollButtons() {
    var carousel = Carousel.create("demo", "Demo").slide("One", "a", p().text("1"));
    var css = PageCompiler.compile(new PageDocument("C", main().child(carousel))).cssContent();

    assertThat(css)
        .contains("::scroll-button(inline-start){content:\"‹\" / \"Previous slide\"")
        .contains("::scroll-button(inline-end){content:\"›\" / \"Next slide\"")
        .contains("::scroll-button(*):disabled");
  }

  @Test
  @DisplayName("Textarea grows with content via field-sizing")
  void textareaFieldSizing() {
    var css =
        PageCompiler.compile(new PageDocument("T", main().child(Forms.textareaControl("m", 3))))
            .cssContent();
    assertThat(css).contains("field-sizing:content").contains("min-block-size:calc(3lh");
  }

  @Test
  @DisplayName("Site pages declare same-site speculation rules unless disabled")
  void speculationRules() {
    var routes =
        List.of(
            new SiteRoute("/", new PageDocument("Home", main().child(a("/b").text("B")))),
            new SiteRoute("/b", new PageDocument("B", main().child(a("/").text("Home")))));

    var html = SiteCompiler.compile(SiteDefinition.of(routes)).pagesByRoute().get("/").html();
    var rules = Jsoup.parse(html).selectFirst("head > script[type=speculationrules]");
    assertThat(rules).isNotNull();
    assertThat(rules.data()).contains("\"prerender\"").contains("\"eagerness\":\"moderate\"");

    var off = SiteDefinition.builder().routes(routes).linkSpeculation(LinkSpeculation.NONE).build();
    assertThat(SiteCompiler.compile(off).pagesByRoute().get("/").html())
        .doesNotContain("speculationrules");
  }

  @Test
  @DisplayName("View keys are content-addressed, so pages with different @scope CSS never clash")
  void crossPageViewScopeConflict() {
    var routes =
        List.of(
            new SiteRoute(
                "/",
                new PageDocument(
                    "A",
                    main()
                        .child(
                            CardGrid.of("g", CssLength.rem(18), CssLength.rem(1), p().text("a"))))),
            new SiteRoute(
                "/b",
                new PageDocument(
                    "B",
                    main()
                        .child(
                            CardGrid.of(
                                "g", CssLength.rem(15), CssLength.rem(1), p().text("b"))))));
    var site = SiteCompiler.compile(SiteDefinition.of(routes));

    String viewA = viewKey(site.pagesByRoute().get("/").html(), "card-grid");
    String viewB = viewKey(site.pagesByRoute().get("/b").html(), "card-grid");
    assertThat(viewA).isNotEqualTo(viewB);
    assertThat(site.sharedCssContent())
        .contains("@scope ([data-view=\"" + viewA + "\"])")
        .contains("@scope ([data-view=\"" + viewB + "\"])");
  }

  private static String viewKey(String html, String viewType) {
    return Jsoup.parse(html).selectFirst("[data-view^=" + viewType + "-]").attr("data-view");
  }

  @Test
  @DisplayName("Site tokens drive both rendering and the shared stylesheet of every page")
  void siteTokensReachRenderingAndCss() {
    var brand = new DesignToken("brand-hue", "oklch(60% 0.2 300)");
    RenderOnce tokenProbe =
        () ->
            p().text(RenderContext.current().tokens().all().contains(brand) ? "brand" : "default");
    var site =
        SiteCompiler.compile(
            SiteDefinition.builder()
                .tokens(new Tokens().register(brand))
                .route(new SiteRoute("/", new PageDocument("Home", main().child(tokenProbe))))
                .build());

    assertThat(site.pagesByRoute().get("/").html()).contains("<p>brand</p>");
    assertThat(site.sharedCssContent()).contains("--brand-hue:oklch(60% 0.2 300)");
  }

  @Test
  @DisplayName("Reveal-once is a scroll-triggered animation gated behind reduced motion")
  void textScaleAndRevealOnce() {
    var compiled =
        PageCompiler.compile(
            new PageDocument(
                "R", main().child(p().revealOnce(ScrollReveal.FADE_UP).text("hello"))));
    String rule =
        CssBlocks.classRule(
            compiled.cssContent(),
            CssBlocks.generatedClass(
                Jsoup.parse(compiled.html()).selectFirst("main p").className(), "i-"));
    assertThat(CssBlocks.block(rule, "@media (prefers-reduced-motion: no-preference){"))
        .contains("timeline-trigger:")
        .contains("animation-trigger:")
        .contains("play-once");
    assertThat(compiled.cssContent()).contains("@keyframes tessera-fade-up");
  }

  @Test
  @DisplayName("Sites write and serve pages, assets and generated files; collisions are rejected")
  void siteAssets(@TempDir Path out) throws Exception {
    var home =
        new PageDocument(
            "Home",
            main()
                .children(
                    img("/assets/logo.svg", "Logo").dimensions(1, 1), a("/guide").text("Guide")));
    var routes =
        List.of(
            new SiteRoute("/", home),
            new SiteRoute("/guide", new PageDocument("Guide", main().text("Guide"))));

    assertThatThrownBy(() -> SiteCompiler.compile(SiteDefinition.of(routes)))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("/assets/logo.svg");

    var site =
        SiteCompiler.compile(
            SiteDefinition.builder()
                .routes(routes)
                .asset(SiteAsset.text("/assets/logo.svg", "<svg/>"))
                .build());
    SiteCompiler.writeToDirectory(site, out);
    assertThat(Files.readString(out.resolve("assets/logo.svg"))).isEqualTo("<svg/>");
    assertThat(out.resolve("guide/index.html")).exists();
    assertThat(out.resolve(site.sharedCssPath().substring(1))).exists();

    try (var server = SitePreviewServer.start(site, 0);
        var client = HttpClient.newHttpClient()) {
      var logo = get(client, server.baseUrl() + "/assets/logo.svg");
      assertThat(logo.statusCode()).isEqualTo(200);
      assertThat(logo.headers().firstValue("Content-Type")).hasValue("image/svg+xml");
      assertThat(logo.body()).isEqualTo("<svg/>");

      assertThat(
              get(client, server.baseUrl() + site.sharedCssPath())
                  .headers()
                  .firstValue("Content-Type"))
          .hasValue("text/css; charset=utf-8");
      assertThat(get(client, server.baseUrl() + "/guide/index.html").body())
          .isEqualTo(site.pagesByRoute().get("/guide").html());
      assertThat(get(client, server.baseUrl() + "/missing").statusCode()).isEqualTo(404);
    }

    assertThatThrownBy(
            () ->
                SiteCompiler.compile(
                    SiteDefinition.builder()
                        .routes(routes)
                        .asset(SiteAsset.text("/assets/logo.svg", "<svg/>"))
                        .asset(SiteAsset.text("/search-index.json", "[]"))
                        .build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("/search-index.json");
    assertThatThrownBy(
            () ->
                SiteCompiler.compile(
                    SiteDefinition.builder()
                        .routes(routes)
                        .asset(SiteAsset.text("/guide", "x"))
                        .build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("collides with route");
  }

  private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString());
  }
}
