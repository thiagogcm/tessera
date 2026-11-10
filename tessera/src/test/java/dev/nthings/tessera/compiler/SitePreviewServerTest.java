package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.SiteFixtures.LOGO_CONTENT;
import static dev.nthings.tessera.support.SiteFixtures.LOGO_PATH;
import static dev.nthings.tessera.support.SiteFixtures.siteWithLogo;
import static dev.nthings.tessera.support.SiteFixtures.stylesheetPath;

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
    site = Compiler.compile(siteWithLogo());
    server = SitePreviewServer.start(site, 0);
    client = HttpClient.newHttpClient();
  }

  private static HttpResponse<String> get(String path) throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder(URI.create(server.baseUrl() + path)).build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  @Test
  @DisplayName("serves registered assets with their content type")
  void servesAssetsWithContentType() throws Exception {
    var logo = get(LOGO_PATH);

    assertThat(logo.statusCode()).isEqualTo(200);
    assertThat(logo.headers().firstValue("Content-Type")).hasValue("image/svg+xml");
    assertThat(logo.body()).isEqualTo(LOGO_CONTENT);
  }

  @Test
  @DisplayName("serves the shared stylesheet as UTF-8 CSS")
  void servesStylesheetAsCss() throws Exception {
    var stylesheet = get(stylesheetPath(site));

    assertThat(stylesheet.headers().firstValue("Content-Type")).hasValue("text/css; charset=utf-8");
  }

  @Test
  @DisplayName("serves a route at its index.html path")
  void servesRouteIndexHtml() throws Exception {
    assertThat(get("/guide/index.html").body()).isEqualTo(site.pages().get("/guide"));
  }

  @Test
  @DisplayName("answers 404 for an unknown path")
  void returns404ForUnknownPath() throws Exception {
    assertThat(get("/missing").statusCode()).isEqualTo(404);
  }

  @Test
  @DisplayName("redirects a directory route to its trailing-slash URL, keeping the query")
  void redirectsKeepingQuery() throws Exception {
    var response = get("/guide?q=a%2Bb");

    assertThat(response.statusCode()).isEqualTo(308);
    assertThat(response.headers().firstValue("Location")).hasValue("/guide/?q=a%2Bb");
    assertThat(get("/guide/?q=a%2Bb").body()).isEqualTo(site.pages().get("/guide"));
  }
}
