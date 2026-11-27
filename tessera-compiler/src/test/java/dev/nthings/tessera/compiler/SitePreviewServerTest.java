package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.SiteFixtures.docsSite;
import static dev.nthings.tessera.support.SiteFixtures.logo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
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
                docsSite()
                    .withAssets(
                        List.of(logo(), Asset.of("/assets/manual v1.pdf", new byte[] {1, 2}))));
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
    assertThat(site.assets()).hasSizeGreaterThan(3);
    for (var file : site.assets().values()) {
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

  @Test
  @DisplayName("watch mode rebuilds on a file change, keeps the last good site, and reloads pages")
  void rebuildsWatchedSources(@TempDir Path sources) throws Exception {
    var content = Files.writeString(sources.resolve("home.txt"), "first");
    var builds = new AtomicInteger();
    Supplier<CompiledSite> build =
        () -> {
          builds.incrementAndGet();
          var text = readString(content);
          if (text.equals("broken")) {
            throw new IllegalStateException("broken content");
          }
          return Compiler.forSite()
              .compile(
                  SiteDefinition.of(SiteRoute.of("/", PageDocument.of("Home", main().text(text)))));
        };

    try (var watching = SitePreviewServer.watch(0, build, sources)) {
      assertThat(fetch(watching, "/")).contains("first").contains("src=\"/_tessera/live.js?v=1\"");
      assertThat(fetch(watching, "/_tessera/live.js")).contains("location.reload()");

      Files.writeString(content, "broken");
      await(() -> builds.get() > 1);
      assertThat(fetch(watching, "/_tessera/version")).isEqualTo("1");
      assertThat(fetch(watching, "/")).contains("first");

      Files.writeString(content, "second");
      await(() -> !fetch(watching, "/_tessera/version").equals("1"));
      assertThat(fetch(watching, "/")).contains("second");

      var version = fetch(watching, "/_tessera/version");
      var posts = Files.createDirectory(sources.resolve("posts"));
      await(() -> !fetch(watching, "/_tessera/version").equals(version));
      var afterDirectory = fetch(watching, "/_tessera/version");
      Files.writeString(posts.resolve("a.txt"), "nested");
      await(() -> !fetch(watching, "/_tessera/version").equals(afterDirectory));
    }
  }

  private interface Condition {
    boolean holds() throws Exception;
  }

  private static void await(Condition condition) throws Exception {
    var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
    while (!condition.holds()) {
      if (System.nanoTime() > deadline) {
        throw new AssertionError("timed out waiting for a rebuild");
      }
      Thread.sleep(25);
    }
  }

  private static String readString(Path file) {
    try {
      return Files.readString(file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String fetch(SitePreviewServer target, String path)
      throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder(URI.create(target.baseUrl() + path)).build();
    return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
  }
}
