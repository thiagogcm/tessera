package dev.nthings.tessera.site;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.SiteFixtures.documentationRoutes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Generated site artifacts")
class SiteArtifactsTest {

  private static SiteCompiler.CompiledSite site;

  @BeforeAll
  static void compileSite() {
    site =
        SiteCompiler.compile(
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
      var withoutBaseUrl = SiteCompiler.compile(SiteDefinition.of(documentationRoutes()));

      assertThat(withoutBaseUrl.sitemapXml()).isNull();
    }
  }

  @Nested
  @DisplayName("search-index.json")
  class SearchIndex {

    /// The index with whitespace around JSON punctuation removed, so assertions don't depend on
    /// the layout but still see spaces inside values.
    private String compactIndex() {
      return site.searchIndexJson().replaceAll("\\s*([{}\\[\\]:,])\\s*", "$1");
    }

    @Test
    @DisplayName("prefers the route title over the document title")
    void prefersRouteTitleOverDocumentTitle() {
      assertThat(compactIndex()).contains("\"path\":\"/guide\",\"title\":\"Getting Started\"");
    }

    @Test
    @DisplayName("lists table-of-contents sections with their level")
    void listsTableOfContentsSectionsWithLevel() {
      assertThat(compactIndex())
          .contains("{\"id\":\"config\",\"title\":\"Configuration\",\"level\":3}");
    }
  }
}
