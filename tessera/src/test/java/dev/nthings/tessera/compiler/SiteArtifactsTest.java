package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.SiteFixtures.documentationRoutes;

import dev.nthings.tessera.components.Card;
import dev.nthings.tessera.components.Heading;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.components.Footnote;
import dev.nthings.tessera.document.components.Footnotes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Generated site artifacts")
class SiteArtifactsTest {

  private static CompiledSite site;

  @BeforeAll
  static void compileSite() {
    site =
        Compiler.compile(
            SiteDefinition.builder()
                .baseUrl("https://tessera.dev/")
                .routes(documentationRoutes())
                .build());
  }

  @Nested
  @DisplayName("sitemap.xml")
  class Sitemap {

    @Test
    @DisplayName("lists each route under the base URL")
    void listsRoutesUnderBaseUrl() {
      var locations =
          site.sitemapXml()
              .lines()
              .filter(line -> line.contains("<loc>"))
              .map(String::strip)
              .toList();

      assertThat(locations)
          .containsExactly(
              "<loc>https://tessera.dev/</loc>",
              "<loc>https://tessera.dev/guide</loc>",
              "<loc>https://tessera.dev/faq</loc>");
    }

    @Test
    @DisplayName("omits the sitemap when the base URL is absent")
    void requiresBaseUrl() {
      var withoutBaseUrl = Compiler.compile(SiteDefinition.of(documentationRoutes()));

      assertThat(withoutBaseUrl.sitemapXml()).isNull();
    }
  }

  @Nested
  @DisplayName("search-index.json")
  class SearchIndex {

    @Test
    @DisplayName("indexes an outlined heading and skips interface headings")
    void indexesOutlinedHeadings() {
      var target = ElementRef.scoped("custom");
      var compiled =
          Compiler.compile(
              SiteDefinition.of(
                  new SiteRoute(
                      "/",
                      new PageDocument(
                          "Custom",
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
                                  DocumentElement.of(p())
                                      .child(Footnote.of("A detail.").reference()))))));

      assertThat(compact(compiled.searchIndexJson()))
          .contains("{\"id\":\"custom\",\"title\":\"Custom section\",\"level\":2}")
          .doesNotContain("Toolbar")
          .doesNotContain("Widget")
          .doesNotContain("Notes");
      assertThat(compiled.pages().get("/"))
          .contains("<code>mvn</code>")
          .doesNotContain(">Custom section<");
    }

    /// JSON with whitespace around punctuation removed; spaces inside values are kept.
    private static String compact(String json) {
      return json.replaceAll("\\s*([{}\\[\\]:,])\\s*", "$1");
    }

    private String compactIndex() {
      return compact(site.searchIndexJson());
    }

    @Test
    @DisplayName("uses the route title, falling back to the document title")
    void prefersRouteTitle() {
      assertThat(compactIndex())
          .contains("\"path\":\"/guide\",\"title\":\"Getting Started\"")
          .contains("\"path\":\"/faq\",\"title\":\"FAQ\"")
          .doesNotContain("\"title\":\"Guide\"");
    }

    @Test
    @DisplayName("lists page sections with their heading level")
    void listsSectionsWithLevel() {
      assertThat(compactIndex())
          .contains("{\"id\":\"features\",\"title\":\"Features\",\"level\":2}")
          .contains("{\"id\":\"quickstart\",\"title\":\"Quick Start\",\"level\":2}")
          .contains("{\"id\":\"config\",\"title\":\"Configuration\",\"level\":3}");
    }
  }
}
