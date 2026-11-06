package dev.nthings.tessera.site;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.SiteFixtures.LOGO_CONTENT;
import static dev.nthings.tessera.support.SiteFixtures.LOGO_PATH;
import static dev.nthings.tessera.support.SiteFixtures.siteWithLogo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@DisplayName("SitePreviewServer")
@Timeout(10)
class SitePreviewServerTest {

  private static CompiledSite site;

  @AutoClose private static HttpClient client;
  @AutoClose private static SitePreviewServer server;

  @BeforeAll
  static void startServer() {
    site = SiteCompiler.compile(siteWithLogo());
    server = SitePreviewServer.start(site, 0);
    client = HttpClient.newHttpClient();
  }

  private static HttpResponse<String> get(String path) throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder(URI.create(server.baseUrl() + path)).build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  @Test
  @DisplayName("serves registered assets with their content type")
  void servesRegisteredAssetsWithContentType() throws Exception {
    var logo = get(LOGO_PATH);

    assertThat(logo.statusCode()).isEqualTo(200);
    assertThat(logo.headers().firstValue("Content-Type")).hasValue("image/svg+xml");
    assertThat(logo.body()).isEqualTo(LOGO_CONTENT);
  }

  @Test
  @DisplayName("serves the shared stylesheet as UTF-8 CSS")
  void servesSharedStylesheetAsCss() throws Exception {
    var stylesheet = get(site.sharedCssPath());

    assertThat(stylesheet.headers().firstValue("Content-Type")).hasValue("text/css; charset=utf-8");
  }

  @Test
  @DisplayName("serves each route at its index.html path")
  void servesRoutesAtIndexHtmlPath() throws Exception {
    assertThat(get("/guide/index.html").body()).isEqualTo(site.pagesByRoute().get("/guide").html());
  }

  @Test
  @DisplayName("answers 404 for unknown paths")
  void answersNotFoundForUnknownPaths() throws Exception {
    assertThat(get("/missing").statusCode()).isEqualTo(404);
  }
}
