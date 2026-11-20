package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.SiteFixtures.docsSite;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("SitePreviewServer")
@Timeout(10)
class SitePreviewServerTest {

  private static CompiledSite site;

  @AutoClose private static HttpClient client;
  @AutoClose private static SitePreviewServer server;

  @BeforeAll
  static void startServer() {
    site =
        Compiler.forSite()
            .compile(
                docsSite().asset(Asset.of("/assets/manual v1.pdf", new byte[] {1, 2})).build());
    server = SitePreviewServer.start(site, 0);
    client = HttpClient.newHttpClient();
  }

  private static HttpResponse<byte[]> get(String rawPath) throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder(URI.create(server.baseUrl() + rawPath)).build();
    return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
  }

  @Test
  @DisplayName("serves every published file at its URL with its content type and bytes")
  void servesEveryFile() throws Exception {
    assertThat(site.files()).hasSizeGreaterThan(3);
    for (var file : site.files().values()) {
      var response = get(new URI(null, null, file.path(), null).toASCIIString());

      assertThat(response.statusCode()).as(file.path()).isEqualTo(200);
      assertThat(response.headers().firstValue("Content-Type")).hasValue(file.contentType());
      assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
      assertThat(response.body()).isEqualTo(file.content());
    }
  }

  @ParameterizedTest
  @CsvSource({"/, /", "/guide/, /guide", "/guide/index.html, /guide", "/notes.html, /notes.html"})
  @DisplayName("serves a page at its published URL and its index.html alias")
  void servesPages(String url, String route) throws Exception {
    var response = get(url);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type")).hasValue("text/html; charset=utf-8");
    assertThat(new String(response.body(), StandardCharsets.UTF_8))
        .isEqualTo(site.pages().get(route));
  }

  @Test
  @DisplayName("redirects a directory route to its trailing-slash URL, keeping the query")
  void redirectsKeepingQuery() throws Exception {
    var response = get("/guide/install?q=a%2Bb");

    assertThat(response.statusCode()).isEqualTo(308);
    assertThat(response.headers().firstValue("Location")).hasValue("/guide/install/?q=a%2Bb");
  }

  @Test
  @DisplayName("answers 404 for an unknown path")
  void answersNotFound() throws Exception {
    assertThat(get("/missing").statusCode()).isEqualTo(404);
    assertThat(get("/assets/missing.png").statusCode()).isEqualTo(404);
  }
}
