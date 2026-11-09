package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.SiteFixtures.documentationRoutes;

import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
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
    void listsEachRouteUnderBaseUrl() {
      assertThat(site.sitemapXml())
          .contains("<loc>https://tessera.dev/</loc>")
          .contains("<loc>https://tessera.dev/guide</loc>");
    }

    @Test
    @DisplayName("is not generated without a base URL")
    void isNotGeneratedWithoutBaseUrl() {
      var withoutBaseUrl = Compiler.compile(SiteDefinition.of(documentationRoutes()));

      assertThat(withoutBaseUrl.sitemapXml()).isNull();
    }
  }

  @Nested
  @DisplayName("search-index.json")
  class SearchIndex {

    @Test
    void indexesHeadingsFromCustomComponents() {
      Component custom =
          context -> {
            var target = ElementRef.scoped("custom");
            context.registerHeading(target, "Custom section", 2);
            return Elements.h2().id(target).text("Custom section");
          };
      var compiled =
          Compiler.compile(
              SiteDefinition.of(new SiteRoute("/", new PageDocument("Custom", custom))));

      assertThat(compiled.searchIndexJson())
          .contains("\"id\": \"custom\"")
          .contains("\"title\": \"Custom section\"")
          .contains("\"level\": 2");
    }

    /// Index with whitespace around JSON punctuation removed; spaces inside values are kept.
    private String compactIndex() {
      return site.searchIndexJson().replaceAll("\\s*([{}\\[\\]:,])\\s*", "$1");
    }

    @Test
    @DisplayName("prefers the route title over the document title")
    void prefersRouteTitleOverDocumentTitle() {
      assertThat(compactIndex())
          .contains("\"path\":\"/guide\"")
          .contains("\"title\":\"Getting Started\"");
    }

    @Test
    @DisplayName("lists table-of-contents sections with their level")
    void listsTableOfContentsSectionsWithLevel() {
      assertThat(compactIndex())
          .contains("\"id\":\"config\"")
          .contains("\"title\":\"Configuration\"")
          .contains("\"level\":3");
    }
  }
}
