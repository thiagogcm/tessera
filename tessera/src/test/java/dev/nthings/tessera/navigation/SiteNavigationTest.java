package dev.nthings.tessera.navigation;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static dev.nthings.tessera.support.SiteFixtures.documentationRoutes;

import dev.nthings.tessera.compiler.SiteDefinition;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SiteNavigation")
class SiteNavigationTest {

  private final SiteDefinition site = SiteDefinition.of(documentationRoutes());

  @Nested
  @DisplayName("docPager")
  class DocPagerLinks {

    @Test
    @DisplayName(
        "links to the neighbouring routes, preferring route titles and omitting missing sides")
    void linksToNeighbouringRoutes() {
      var links = cardsAt("/guide");

      assertThat(links)
          .extracting(a -> a.attr("href") + " " + a.select(".pager-title").text())
          .containsExactly("/ Tessera Home", "/faq FAQ");
      assertThat(links)
          .extracting(a -> a.select(".pager-hint").text())
          .containsExactly("← Previous", "Next →");
      assertThat(cardsAt("/"))
          .extracting(a -> a.attr("href") + " " + a.select(".pager-hint").text())
          .containsExactly("/guide Next →");
      assertThat(cardsAt("/faq"))
          .extracting(a -> a.attr("href") + " " + a.select(".pager-hint").text())
          .containsExactly("/guide ← Previous");
      assertThat(cardsAt("/guide/")).extracting(a -> a.attr("href")).containsExactly("/", "/faq");
    }

    @Test
    @DisplayName("rejects an unknown route")
    void rejectsUnknownRoute() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> SiteNavigation.docPager(site, "/missing"))
          .withMessage("No route registered at /missing");
    }

    private Elements cardsAt(String path) {
      return renderMain(SiteNavigation.docPager(site, path)).elements("nav a");
    }
  }

  @Test
  @DisplayName(
      "searchPalette indexes every route in one group with its title, path, and description")
  void indexesEveryRoute() {
    var page = renderMain(SiteNavigation.searchPalette(site));
    var options = page.elements("a.palette-item");

    assertThat(page.element(".palette-group-title").text()).isEqualTo("Pages & Guides");
    assertThat(options).extracting(o -> o.attr("href")).containsExactly("/", "/guide", "/faq");
    assertThat(options)
        .extracting(o -> o.select(".palette-item-title").text())
        .containsExactly("Tessera Home", "Getting Started", "FAQ");
    assertThat(options)
        .extracting(o -> o.select(".palette-item-badge").text())
        .containsExactly("/", "/guide", "/faq");
    assertThat(options.getFirst().select(".palette-item-desc").text())
        .isEqualTo("Static HTML/CSS compiler");
    assertThat(options.get(1).select(".palette-item-desc")).isEmpty();
  }
}
