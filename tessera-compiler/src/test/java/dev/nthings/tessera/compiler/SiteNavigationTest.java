package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.HtmlAssertions.tuple;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static dev.nthings.tessera.support.SiteFixtures.documentationRoutes;

import dev.nthings.tessera.document.DocumentComponent;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.PageAddress;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SiteNavigation")
class SiteNavigationTest {

  private final SiteDefinition site = SiteDefinition.of(documentationRoutes());

  /// The route a rendered link leads to.
  private String routeOf(Element link) {
    return site.route(link.attr("href")).map(SiteRoute::path).orElseThrow();
  }

  private Elements pagerLinks(String path) {
    return renderMain(SiteNavigation.docPager(site.routes(), PageAddress.of(path)))
        .elements("nav a");
  }

  @Test
  @DisplayName("the pager links the neighbouring routes in order under their display titles")
  void pagerLinksNeighbouringRoutes() {
    assertThat(pagerLinks("/guide"))
        .extracting(this::routeOf, this::side)
        .containsExactly(tuple("/", "Previous"), tuple("/faq", "Next"));
    assertThat(pagerLinks("/guide"))
        .extracting(Element::text)
        .satisfiesExactly(
            previous -> assertThat(previous).contains("Tessera Home"),
            next -> assertThat(next).contains("FAQ"));
    assertThat(pagerLinks("/"))
        .extracting(this::routeOf, this::side)
        .containsExactly(tuple("/guide", "Next"));
    assertThat(pagerLinks("/faq/"))
        .extracting(this::routeOf, this::side)
        .containsExactly(tuple("/guide", "Previous"));
  }

  /// Which side of the pager a link is on, from its visible hint.
  private String side(Element link) {
    return link.text().contains("Previous")
        ? "Previous"
        : link.text().contains("Next") ? "Next" : "";
  }

  @Test
  @DisplayName("the pager rejects an unknown route")
  void pagerRejectsUnknownRoute() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SiteNavigation.docPager(site.routes(), PageAddress.of("/missing")));
  }

  @Test
  @DisplayName("the search palette links every route with its title and description")
  void paletteLinksEveryRoute() {
    var links =
        renderMain(SiteNavigation.searchPalette(site.routes(), "Pages")).elements("a[href]");

    assertThat(links).extracting(this::routeOf).containsExactly("/", "/guide", "/faq");
    assertThat(links.get(0).text()).contains("Tessera Home", "Static HTML/CSS compiler");
    assertThat(links.get(1).text()).contains("Getting Started");
    assertThat(links.get(2).text()).contains("FAQ");
  }

  @Test
  @DisplayName("a shared layout builds pager and palette links that pass validation on every route")
  void navigationPassesSiteLinkValidation() {
    DocumentComponent layout =
        context ->
            DocumentContent.of(
                main()
                    .children(
                        SiteNavigation.searchPalette(site.routes(), "Pages"),
                        SiteNavigation.docPager(site.routes(), context.address().orElseThrow())));
    var withNavigation =
        SiteDefinition.of(
            site.routes().stream()
                .map(route ->
                    SiteRoute.of(route.path(), PageDocument.of(route.document().title(), layout)))
                .toList());

    var compiled = Compiler.forSite().compile(withNavigation);
    assertThat(Jsoup.parse(compiled.pages().get("/guide")).select("nav a"))
        .extracting(link -> link.attr("href"))
        .containsExactly("/", "/faq/");
  }

  @Test
  @DisplayName("links use published document URIs with trailing slashes and percent-encoding")
  void linksUsePublishedDocumentUris() {
    var routes =
        List.of(
            SiteRoute.of("/", PageDocument.of("Home", main().text("Home"))),
            SiteRoute.of("/guides/café", PageDocument.of("Café", main().text("Café"))),
            SiteRoute.of("/about.html", PageDocument.of("About", main().text("About"))));

    var paletteLinks = renderMain(SiteNavigation.searchPalette(routes, "Docs")).elements("a[href]");
    assertThat(paletteLinks)
        .extracting(link -> link.attr("href"))
        .containsExactly("/", "/guides/caf%C3%A9/", "/about.html");

    var pagerLinks =
        renderMain(SiteNavigation.docPager(routes, PageAddress.of("/guides/café")))
            .elements("nav a");
    assertThat(pagerLinks)
        .extracting(link -> link.attr("href"))
        .containsExactly("/", "/about.html");
  }
}
