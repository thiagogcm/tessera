package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.tuple;
import static dev.nthings.tessera.support.SiteFixtures.documentationRoutes;

import dev.nthings.tessera.components.Card;
import dev.nthings.tessera.components.Heading;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.components.Footnote;
import dev.nthings.tessera.document.components.Footnotes;
import dev.nthings.tessera.support.Json;
import dev.nthings.tessera.support.RenderedPage;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Generated site artifacts")
class SiteArtifactsTest {

  private static SiteRoute page(String path) {
    return SiteRoute.of(path, PageDocument.of(path, main().text(path)));
  }

  @Test
  @DisplayName("the sitemap lists each page's published URL under the base URL, ASCII-encoded")
  void listsPublishedUrlsInTheSitemap() {
    var routes = List.of(page("/"), page("/guide"), page("/guides/café"), page("/notes.html"));
    var site =
        Compiler.forSite()
            .compile(SiteDefinition.of(routes).withBaseUrl(URI.create("https://tessera.dev/")));

    var sitemap = Jsoup.parse(site.sitemapXml(), Parser.xmlParser());
    assertThat(sitemap.select("urlset > url > loc"))
        .extracting(Element::text)
        .containsExactly(
            "https://tessera.dev/",
            "https://tessera.dev/guide/",
            "https://tessera.dev/guides/caf%C3%A9/",
            "https://tessera.dev/notes.html");
    assertThat(site.assets().get("/sitemap.xml").textContent()).isEqualTo(site.sitemapXml());
  }

  @Test
  @DisplayName("with a base URL each page links its published URL as canonical unless it sets one")
  void linksCanonicalUrlsUnderTheBaseUrl() {
    var own = URI.create("https://example.com/original/");
    var site =
        Compiler.forSite()
            .compile(
                SiteDefinition.of(
                        page("/"),
                        page("/guides/café"),
                        SiteRoute.of(
                            "/copy",
                            PageDocument.of("Copy", main().text("copy")).withCanonicalUrl(own)))
                    .withBaseUrl(URI.create("https://tessera.dev/")));

    assertThat(canonicalHref(site, "/")).isEqualTo("https://tessera.dev/");
    assertThat(canonicalHref(site, "/guides/café"))
        .isEqualTo("https://tessera.dev/guides/caf%C3%A9/");
    assertThat(canonicalHref(site, "/copy")).isEqualTo(own.toString());
  }

  @Test
  @DisplayName("without a base URL pages have no canonical link of their own")
  void omitsCanonicalUrlsWithoutBaseUrl() {
    var site = Compiler.forSite().compile(SiteDefinition.of(page("/"), page("/guide")));

    assertThat(Jsoup.parse(site.pages().get("/guide")).select("link[rel=canonical]")).isEmpty();
  }

  @Test
  @DisplayName("the document description feeds the meta tag and the search index")
  void sharesTheDocumentDescription() {
    var routes =
        List.of(
            SiteRoute.of(
                "/", PageDocument.of("Home", main().text("x")).withDescription("Start here")));
    var site = Compiler.forSite().compile(SiteDefinition.of(routes));

    assertThat(
            Jsoup.parse(site.pages().get("/"))
                .selectFirst("head > meta[name=description]")
                .attr("content"))
        .isEqualTo("Start here");
    assertThat(Json.parse(site.searchIndexJson()).list().getFirst().get("description").string())
        .isEqualTo("Start here");
  }

  private static String canonicalHref(CompiledSite site, String route) {
    return Jsoup.parse(site.pages().get(route)).selectFirst("link[rel=canonical]").attr("href");
  }

  @Test
  @DisplayName("without a base URL no sitemap is published")
  void omitsTheSitemapWithoutBaseUrl() {
    var site = Compiler.forSite().compile(SiteDefinition.of(documentationRoutes()));

    assertThat(site.sitemapXml()).isNull();
    assertThat(site.assets()).doesNotContainKey("/sitemap.xml");
  }

  @Test
  @DisplayName("the search index lists each route's title, description and outlined sections")
  void indexesRoutesAndSections() {
    var site = SiteDefinition.of(documentationRoutes());
    var index = Json.parse(Compiler.forSite().compile(site).searchIndexJson()).list();

    assertThat(index)
        .extracting(
            entry -> site.route(entry.get("path").string()).orElseThrow().path(),
            entry -> entry.get("title").string(),
            entry -> entry.keys().contains("description"))
        .containsExactly(
            tuple("/", "Tessera Home", true),
            tuple("/guide", "Getting Started", false),
            tuple("/faq", "FAQ", false));
    assertThat(index.getFirst().get("description").string()).isEqualTo("Static HTML/CSS compiler");
    assertThat(index.get(1).get("headings").list())
        .extracting(
            heading -> heading.get("id").string(),
            heading -> heading.get("title").string(),
            heading -> heading.get("level").intValue())
        .containsExactly(
            tuple("quickstart", "Quick Start", 2), tuple("config", "Configuration", 3));
    assertThat(index.get(2).get("headings").list()).isEmpty();
  }

  @Test
  @DisplayName("indexed sections point at anchors on the page and skip interface headings")
  void indexesOnlyOutlinedHeadings() {
    var target = ElementRef.scoped("custom");
    var body =
        DocumentElement.of(
                main()
                    .children(
                        Heading.of(
                                2,
                                "Custom section",
                                Elements.code().text("mvn"),
                                Elements.text(" install"))
                            .id(target)
                            .outline(target),
                        h2().text("Toolbar"),
                        Card.of("Widget").headingLevel(2)))
            .children(
                Footnotes.of().title("Notes"),
                DocumentElement.of(p()).child(Footnote.of("A detail.").reference()));
    var site =
        Compiler.forSite()
            .compile(SiteDefinition.of(SiteRoute.of("/", PageDocument.of("Custom", body))));

    var headings = Json.parse(site.searchIndexJson()).list().getFirst().get("headings").list();
    assertThat(headings)
        .extracting(heading -> heading.get("title").string())
        .containsExactly("Custom section");
    var anchor = RenderedPage.of(site, "/").elementById(headings.getFirst().get("id").string());
    assertThat(anchor.text()).isEqualTo("mvn install");
  }

  @Test
  @DisplayName(
      "titles and descriptions with quotes, backslashes and control characters stay valid JSON")
  void escapesSearchIndexStrings() {
    var title = "Say \"hi\" \\ café";
    var description = "Col1\tCol2\nRow\u0001\u001f end";
    var site =
        Compiler.forSite()
            .compile(
                SiteDefinition.of(
                    SiteRoute.of(
                            "/",
                            PageDocument.of("Home", main().text("x")).withDescription(description))
                        .withTitle(title)));

    var entry = Json.parse(site.searchIndexJson()).list().getFirst();
    assertThat(entry.get("title").string()).isEqualTo(title);
    assertThat(entry.get("description").string()).isEqualTo(description);
  }

  @Test
  @DisplayName("native-image resource metadata covers every bundled compiler script")
  void nativeImageResourceConfigCoversBundledScripts() throws IOException {
    String configJson;
    try (var input =
        SiteArtifactsTest.class.getResourceAsStream(
            "/META-INF/native-image/dev.nthings/tessera-compiler/resource-config.json")) {
      assertThat(input).isNotNull();
      configJson = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
    var patterns =
        Json.parse(configJson).get("resources").get("includes").list().stream()
            .map(entry -> Pattern.compile(entry.get("pattern").string()))
            .toList();

    var scriptsDir = "dev/nthings/tessera/compiler/";
    List<String> scripts;
    try (var files = Files.list(Path.of("src/main/resources", scriptsDir))) {
      scripts =
          files
              .map(file -> scriptsDir + file.getFileName())
              .filter(name -> name.endsWith(".js"))
              .toList();
    }
    assertThat(scripts).isNotEmpty();
    for (var script : scripts) {
      assertThat(patterns)
          .as("resource-config.json covers %s", script)
          .anyMatch(pattern -> pattern.matcher(script).find());
    }
  }
}
