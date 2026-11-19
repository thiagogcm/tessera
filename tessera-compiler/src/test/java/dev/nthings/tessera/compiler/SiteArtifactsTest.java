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
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Generated site artifacts")
class SiteArtifactsTest {

  private static SiteRoute page(String path) {
    return new SiteRoute(path, new PageDocument(path, main().text(path)));
  }

  @Test
  @DisplayName("the sitemap lists each page's published URL under the base URL, ASCII-encoded")
  void listsPublishedUrlsInTheSitemap() {
    var routes = List.of(page("/"), page("/guide"), page("/guides/café"), page("/notes.html"));
    var site =
        Compiler.compile(
            SiteDefinition.builder().baseUrl("https://tessera.dev/").routes(routes).build());

    var sitemap = Jsoup.parse(site.sitemapXml(), Parser.xmlParser());
    assertThat(sitemap.select("urlset > url > loc"))
        .extracting(Element::text)
        .containsExactly(
            "https://tessera.dev/",
            "https://tessera.dev/guide/",
            "https://tessera.dev/guides/caf%C3%A9/",
            "https://tessera.dev/notes.html");
    assertThat(site.files().get("/sitemap.xml").textContent()).isEqualTo(site.sitemapXml());
  }

  @Test
  @DisplayName("without a base URL no sitemap is published")
  void omitsTheSitemapWithoutBaseUrl() {
    var site = Compiler.compile(SiteDefinition.of(documentationRoutes()));

    assertThat(site.sitemapXml()).isNull();
    assertThat(site.files()).doesNotContainKey("/sitemap.xml");
  }

  @Test
  @DisplayName("the search index lists each route's title, description and outlined sections")
  void indexesRoutesAndSections() {
    var site = SiteDefinition.of(documentationRoutes());
    var index = Json.parse(Compiler.compile(site).searchIndexJson()).list();

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
        Compiler.compile(SiteDefinition.of(new SiteRoute("/", new PageDocument("Custom", body))));

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
        Compiler.compile(
            SiteDefinition.of(
                new SiteRoute(
                    "/", new PageDocument("Home", main().text("x")), title, description)));

    var entry = Json.parse(site.searchIndexJson()).list().getFirst();
    assertThat(entry.get("title").string()).isEqualTo(title);
    assertThat(entry.get("description").string()).isEqualTo(description);
  }
}
