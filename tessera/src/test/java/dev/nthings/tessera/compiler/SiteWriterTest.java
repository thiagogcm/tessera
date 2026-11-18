package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.SiteFixtures.docsSite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("SiteWriter.writeToDirectory")
class SiteWriterTest {

  @TempDir private Path out;

  @Test
  @DisplayName("every page and file lands at its URL path with its exact bytes, and nothing else")
  void writesEveryPageAndFileAtItsUrlPath() throws IOException {
    var site = Compiler.compile(docsSite().baseUrl("https://example.com").build());

    SiteWriter.writeToDirectory(site, out.resolve("site"));

    var root = out.resolve("site");
    var pageFiles =
        Map.of(
            "/", "index.html",
            "/guide", "guide/index.html",
            "/guide/install", "guide/install/index.html",
            "/notes.html", "notes.html");
    assertThat(site.pages().keySet()).containsExactlyInAnyOrderElementsOf(pageFiles.keySet());
    pageFiles.forEach((route, file) ->
        assertThat(root.resolve(file))
            .hasBinaryContent(site.pages().get(route).getBytes(StandardCharsets.UTF_8)));
    site.files()
        .forEach((path, file) ->
            assertThat(root.resolve(path.substring(1))).hasBinaryContent(file.content()));
    try (var written = Files.walk(root)) {
      assertThat(written.filter(Files::isRegularFile))
          .hasSize(site.pages().size() + site.files().size());
    }
  }

  @Test
  @DisplayName("writing over an earlier build replaces its files")
  void overwritesAnEarlierBuild() throws IOException {
    var site = Compiler.compile(docsSite().build());
    Files.writeString(out.resolve("index.html"), "stale");

    SiteWriter.writeToDirectory(site, out);

    assertThat(out.resolve("index.html")).hasContent(site.pages().get("/"));
  }

  @Test
  @DisplayName("the current directory is a valid output directory")
  void writesToTheCurrentDirectory() throws IOException {
    var page = "site-writer-" + UUID.randomUUID() + ".html";
    var site =
        Compiler.compile(
            SiteDefinition.builder()
                .outputMode(OutputMode.inline())
                .linkSpeculation(LinkSpeculation.NONE)
                .route(new SiteRoute(page, new PageDocument("Here", main().text("Here"))))
                .build());
    var written = site.files().keySet().stream().map(path -> Path.of(path.substring(1))).toList();
    assertThat(written).noneMatch(Files::exists);

    try {
      SiteWriter.writeToDirectory(site, Path.of("."));

      assertThat(Path.of(page)).hasContent(site.pages().get("/" + page));
    } finally {
      Files.deleteIfExists(Path.of(page));
      for (var file : written) {
        Files.deleteIfExists(file);
      }
    }
  }
}
