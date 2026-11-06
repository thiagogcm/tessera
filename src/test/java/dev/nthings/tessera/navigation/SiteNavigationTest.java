package dev.nthings.tessera.navigation;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static dev.nthings.tessera.support.SiteFixtures.documentationRoutes;

import dev.nthings.tessera.components.DocPager;
import dev.nthings.tessera.render.PageDocument;
import dev.nthings.tessera.site.SiteDefinition;
import dev.nthings.tessera.site.SiteRoute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SiteNavigation")
class SiteNavigationTest {

  @Nested
  @DisplayName("docPager")
  class DocPagerLinks {

    private final SiteDefinition site = SiteDefinition.of(documentationRoutes());

    @Test
    @DisplayName("links to the neighbouring routes, preferring route titles")
    void linksToNeighbouringRoutes() {
      var pager = SiteNavigation.docPager(site, "/guide");

      assertThat(pager.prev()).isEqualTo(new DocPager.PageRef("/", "Tessera Home"));
      assertThat(pager.next()).isEqualTo(new DocPager.PageRef("/faq", "FAQ"));
    }

    @Test
    @DisplayName("rejects an unknown route")
    void rejectsUnknownRoute() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> SiteNavigation.docPager(site, "/missing"));
    }
  }

  @Test
  @DisplayName("searchPalette indexes every route, preferring the route title")
  void searchPaletteIndexesEveryRoute() {
    var site =
        SiteDefinition.of(
            new SiteRoute("/", new PageDocument("Home Page", div()), "Home"),
            new SiteRoute("/docs", new PageDocument("Documentation", div())));

    var options = renderMain(SiteNavigation.searchPalette(site)).elements("datalist option");

    assertThat(options).extracting(o -> o.attr("value")).containsExactly("/", "/docs");
    assertThat(options.getFirst().text()).startsWith("Home ");
    assertThat(options.getLast().text()).startsWith("Documentation ");
  }
}
