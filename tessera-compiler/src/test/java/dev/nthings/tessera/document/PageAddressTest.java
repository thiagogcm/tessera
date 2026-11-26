package dev.nthings.tessera.document;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;

import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.compiler.SiteDefinition;
import dev.nthings.tessera.compiler.SiteRoute;
import dev.nthings.tessera.core.AriaCurrent;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.support.RenderedPage;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Page address")
class PageAddressTest {

  private static final URI BASE = URI.create("https://tessera.dev/");

  @ParameterizedTest
  @CsvSource({
    "'/', /, /, https://tessera.dev/",
    "'guide/', /guide, /guide/, https://tessera.dev/guide/",
    "' /guides/café ', /guides/café, /guides/caf%C3%A9/, https://tessera.dev/guides/caf%C3%A9/",
    "'/about.html', /about.html, /about.html, https://tessera.dev/about.html"
  })
  @DisplayName("routes normalize to a document URI and an absolute URL under the base URL")
  void derivesUrlsFromTheRoutePath(
      String raw, String routePath, String documentUri, String absoluteUrl) {
    var address = PageAddress.of(raw).withBaseUrl(BASE);

    assertThat(address.routePath()).isEqualTo(routePath);
    assertThat(address.documentUri().toASCIIString()).isEqualTo(documentUri);
    assertThat(address.absoluteUrl()).hasValue(URI.create(absoluteUrl));
    assertThat(PageAddress.of(raw).absoluteUrl()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/",
        "example.com",
        "ftp://tessera.dev",
        "https://u:p@tessera.dev",
        "https://tessera.dev/?q",
        "https://tessera.dev/docs"
      })
  @DisplayName("a base URL must be an absolute http(s) origin")
  void rejectsInvalidBaseUrls(String baseUrl) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PageAddress.of("/guide").withBaseUrl(URI.create(baseUrl)));
  }

  @ParameterizedTest
  @CsvSource({
    "/guide/, true",
    "/guide, true",
    "/guide/index.html, true",
    "./, true",
    "../guide/?tab=2#install, true",
    "#install, true",
    "?tab=2, true",
    "/, false",
    "/guide/install/, false",
    "/guides/, false",
    "https://tessera.dev/guide/, true",
    "//tessera.dev/guide, true",
    "https://TESSERA.dev:443/guide/, true",
    "http://tessera.dev/guide/, false",
    "https://example.com/guide/, false",
    "'http://[bad', false"
  })
  @DisplayName("isCurrent resolves links against the page URL")
  void resolvesCurrentLinks(String href, boolean current) {
    var address = PageAddress.of("/guide").withBaseUrl(BASE);

    assertThat(address.isCurrent(href)).as(href).isEqualTo(current);
  }

  @Test
  @DisplayName("without a base URL absolute links are never current")
  void absoluteLinksNeedABaseUrl() {
    assertThat(PageAddress.of("/guide").isCurrent("https://tessera.dev/guide/")).isFalse();
    assertThat(PageAddress.of("/guide").isCurrent("/guide/")).isTrue();
  }

  @Test
  @DisplayName("every site route sees its own address, absolute under the base URL")
  void siteRoutesSeeTheirAddress() {
    DocumentComponent layout =
        context -> {
          var address = context.address().orElseThrow();
          var nav =
              List.of("/", "/guide/").stream()
                  .map(href -> {
                    var link = a(href, href);
                    return address.isCurrent(href) ? link.ariaCurrent(AriaCurrent.PAGE) : link;
                  })
                  .toList();
          return DocumentContent.of(
              main()
                  .children(nav)
                  .child(
                      p(address.absoluteUrl().orElseThrow().toString())
                          .id(ElementRef.page("url"))));
        };
    var site =
        Compiler.forSite()
            .compile(
                SiteDefinition.builder()
                    .baseUrl(BASE)
                    .route(SiteRoute.of("/", PageDocument.of("Home", layout)))
                    .route(SiteRoute.of("/guide", PageDocument.of("Guide", layout)))
                    .build());

    var guide = RenderedPage.of(site, "/guide");
    assertThat(guide.element("#url").text()).isEqualTo("https://tessera.dev/guide/");
    assertThat(guide.element("a[aria-current=page]")).hasAttribute("href", "/guide/");
    assertThat(RenderedPage.of(site, "/").element("a[aria-current=page]"))
        .hasAttribute("href", "/");
  }

  @Test
  @DisplayName("a page compiled on its own has no address")
  void standalonePagesHaveNoAddress() {
    DocumentComponent probe =
        context ->
            DocumentContent.of(main(p(context.address().isPresent() ? "routed" : "standalone")));

    var page = RenderedPage.of(PageDocument.of("Alone", probe));

    assertThat(page.element("main > p").text()).isEqualTo("standalone");
  }
}
