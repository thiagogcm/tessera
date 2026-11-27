package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.custom;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.HtmlAssertions.tuple;
import static dev.nthings.tessera.support.SiteFixtures.docsSite;

import dev.nthings.tessera.document.DocumentComponent;
import dev.nthings.tessera.document.PageScript;
import dev.nthings.tessera.document.ScriptModule;
import dev.nthings.tessera.support.Diagnostics;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Host integration")
class HostIntegrationTest {

  private static PageDocument notFound() {
    return PageDocument.of("Not found", main(h1("Page not found"), a("/").text("Home")));
  }

  @Test
  @DisplayName("_headers sends the site headers everywhere and caches hashed files for a year")
  void writesHeadersFile() {
    DocumentComponent widget =
        context -> {
          context.contributeScript(
              PageScript.of("test-chart", "import \"chart\";")
                  .withImports(ScriptModule.of("chart", "export {};")));
          return main().child(custom("test-chart"));
        };
    var site =
        Compiler.forSite()
            .compile(
                SiteDefinition.of(
                        SiteRoute.of("/", PageDocument.of("Home", widget)),
                        SiteRoute.of("/about", PageDocument.of("About", main(h1("About")))))
                    .withAssets(List.of(Asset.text("/assets/logo.svg", "<svg/>"))));
    var stylesheet = Jsoup.parse(site.pages().get("/")).selectFirst("link[rel=stylesheet]");

    assertThat(site.headersFile())
        .isEqualTo(
            """
            /*
              X-Content-Type-Options: nosniff
              Referrer-Policy: strict-origin-when-cross-origin
            %s
              Cache-Control: public, max-age=31536000, immutable
            /assets/scripts/*
              Cache-Control: public, max-age=31536000, immutable
            /assets/modules/*
              Cache-Control: public, max-age=31536000, immutable
            """
                .formatted(stylesheet.attr("href")));
    assertThat(site.assets().get("/_headers").contentType()).isEqualTo("text/plain; charset=utf-8");
    assertThat(site.siteHeaders()).isEqualTo(SiteDefinition.DEFAULT_HEADERS);
  }

  @Test
  @DisplayName("a directory of hashed files only gets one wildcard rule")
  void groupsHashedDirectories() {
    var site = Compiler.forSite().compile(SiteDefinition.of(SiteRoute.of("/", notFound())));

    assertThat(site.headersFile()).contains("/assets/*\n  Cache-Control:");
  }

  @Test
  @DisplayName("custom site headers replace the defaults")
  void customHeaders() {
    var headers = new LinkedHashMap<String, String>();
    headers.put("Content-Security-Policy", "default-src 'self'");
    var site = Compiler.forSite().compile(docsSite().withHeaders(headers));

    assertThat(site.headersFile())
        .startsWith("/*\n  Content-Security-Policy: default-src 'self'\n");
    assertThat(site.headersFile()).doesNotContain("nosniff");
  }

  @Test
  @DisplayName("a header name that is not a token, or a value with a line break, is rejected")
  void rejectsInvalidHeaders() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> docsSite().withHeaders(new LinkedHashMap<>(Map.of("Bad Name", "x"))));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> docsSite().withHeaders(new LinkedHashMap<>(Map.of("X-A", "a\nb"))));
  }

  @Test
  @DisplayName("the not-found page shares the site stylesheet and stays out of sitemap and search")
  void compilesNotFoundPage() {
    var site =
        Compiler.forSite()
            .compile(
                docsSite()
                    .withBaseUrl(URI.create("https://example.com"))
                    .withNotFoundPage(notFound()));

    var page = Jsoup.parse(site.notFoundPage());
    assertThat(page.title()).isEqualTo("Not found");
    assertThat(page.selectFirst("link[rel=stylesheet]").attr("href"))
        .isEqualTo(
            Jsoup.parse(site.pages().get("/")).selectFirst("link[rel=stylesheet]").attr("href"));
    assertThat(page.select("link[rel=canonical]")).isEmpty();
    assertThat(site.pages()).doesNotContainKey("/404.html");
    assertThat(site.sitemapXml()).doesNotContain("404");
    assertThat(site.searchIndexJson()).doesNotContain("404");
  }

  @Test
  @DisplayName("a relative link on the not-found page is reported")
  void reportsRelativeNotFoundLinks() {
    var page = PageDocument.of("Not found", main(a("guide/").text("Guide"), a("#top").text("Top")));

    assertThat(Diagnostics.of(() -> Compiler.forSite().compile(docsSite().withNotFoundPage(page))))
        .extracting(Diagnostics.Found::rule, Diagnostics.Found::route)
        .containsExactly(tuple("not-found-relative-link", "/404.html"));
  }

  @Test
  @DisplayName("a route or asset at a generated host path collides")
  void reportsHostPathCollisions() {
    assertThat(
            Diagnostics.rules(() ->
                Compiler.forSite()
                    .compile(
                        docsSite()
                            .withAssets(List.of(Asset.text("/_headers", "")))
                            .withNotFoundPage(notFound()))))
        .containsExactly("file-path-collision");
    assertThat(
            Diagnostics.rules(() ->
                Compiler.forSite()
                    .compile(
                        SiteDefinition.of(SiteRoute.of("/404.html", notFound()))
                            .withNotFoundPage(notFound()))))
        .containsExactly("file-path-collision");
  }
}
