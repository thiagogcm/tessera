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
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScrollReveal;
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
    assertThat(css).contains("field-sizing:content").contains("min-block-size:calc(3lh + 1rem)");
  }

  @Test
  @DisplayName("Site pages declare same-site speculation rules unless disabled")
  void speculationRules() {
    var routes =
        List.of(
            new SiteRoute("/", new PageDocument("Home", main().child(a("/b").text("B")))),
            new SiteRoute("/b", new PageDocument("B", main().child(a("/").text("Home")))));

    var html = SiteCompiler.compile(new SiteDefinition(routes)).pagesByRoute().get("/").html();
    var rules = Jsoup.parse(html).selectFirst("head > script[type=speculationrules]");
    assertThat(rules).isNotNull();
    assertThat(rules.data()).contains("\"prerender\"").contains("\"eagerness\":\"moderate\"");

    var off = new SiteDefinition(routes).withLinkSpeculation(LinkSpeculation.NONE);
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
                            new CardGrid(
                                "g", CssLength.rem(18), CssLength.rem(1), p().text("a"))))),
            new SiteRoute(
                "/b",
                new PageDocument(
                    "B",
                    main()
                        .child(
                            new CardGrid(
                                "g", CssLength.rem(15), CssLength.rem(1), p().text("b"))))));
    var site = SiteCompiler.compile(new SiteDefinition(routes));

    String viewA = viewKey(site.pagesByRoute().get("/").html(), "card-grid");
    String viewB = viewKey(site.pagesByRoute().get("/b").html(), "card-grid");
    assertThat(viewA).startsWith("card-grid-").isNotEqualTo(viewB);
    assertThat(viewB).startsWith("card-grid-");
    assertThat(site.sharedCssContent())
        .contains("@scope ([data-view=\"" + viewA + "\"])")
        .contains("@scope ([data-view=\"" + viewB + "\"])")
        .contains("min(100%, 15rem)");
  }

  private static String viewKey(String html, String viewType) {
    return Jsoup.parse(html).selectFirst("[data-view^=" + viewType + "-]").attr("data-view");
  }

  @Test
  @DisplayName("Pages follow the OS text scale and reveal-once uses scroll-triggered animation")
  void textScaleAndRevealOnce() {
    var compiled =
        PageCompiler.compile(
            new PageDocument(
                "R", main().child(p().revealOnce(ScrollReveal.FADE_UP).text("hello"))));
    assertThat(Jsoup.parse(compiled.html()).selectFirst("meta[name=text-scale]").attr("content"))
        .isEqualTo("scale");
    assertThat(compiled.cssContent())
        .contains("timeline-trigger:--tessera-reveal view() cover 0% cover 100%")
        .contains("animation-trigger:--tessera-reveal play-once")
        .contains("@keyframes tessera-fade-up");
  }

  @Test
  @DisplayName("Site assets are validated, written to disk, and served")
  void siteAssets(@TempDir Path out) throws Exception {
    var page =
        new PageDocument("Home", main().child(img("/assets/logo.svg", "Logo").dimensions(1, 1)));
    var routes = List.of(new SiteRoute("/", page));

    assertThatThrownBy(() -> SiteCompiler.compile(new SiteDefinition(routes)))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("/assets/logo.svg");

    var site =
        SiteCompiler.compile(
            new SiteDefinition(routes)
                .withAssets(List.of(SiteAsset.text("/assets/logo.svg", "<svg/>"))));
    SiteCompiler.writeToDirectory(site, out);
    assertThat(Files.readString(out.resolve("assets/logo.svg"))).isEqualTo("<svg/>");

    try (var server = SitePreviewServer.start(site, 0);
        var client = HttpClient.newHttpClient()) {
      var res =
          client.send(
              HttpRequest.newBuilder(URI.create(server.baseUrl() + "/assets/logo.svg")).build(),
              HttpResponse.BodyHandlers.ofString());
      assertThat(res.statusCode()).isEqualTo(200);
      assertThat(res.headers().firstValue("Content-Type")).hasValue("image/svg+xml");
      assertThat(res.body()).isEqualTo("<svg/>");
    }

    assertThatThrownBy(
            () ->
                SiteCompiler.compile(
                    new SiteDefinition(routes)
                        .withAssets(
                            List.of(
                                SiteAsset.text("/assets/logo.svg", "<svg/>"),
                                SiteAsset.text("/search-index.json", "[]")))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Duplicate file path: /search-index.json");
  }
}
