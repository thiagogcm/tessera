package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.SiteFixtures.docsSite;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("SiteWriter.writeToDirectory")
class SiteWriterTest {

  @TempDir private Path out;

  @Test
  @DisplayName("every page and file lands at its URL path with its exact bytes, and nothing else")
  void writesEveryPageAndFileAtItsUrlPath() throws IOException {
    var site =
        Compiler.forSite().compile(docsSite().withBaseUrl(URI.create("https://example.com")));

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
    site.assets()
        .forEach((path, file) ->
            assertThat(root.resolve(path.substring(1))).hasBinaryContent(file.content()));
    try (var written = Files.walk(root)) {
      assertThat(written.filter(Files::isRegularFile))
          .hasSize(site.pages().size() + site.assets().size());
    }
  }

  @Test
  @DisplayName("writing over an earlier build replaces its files")
  void overwritesAnEarlierBuild() throws IOException {
    var site = Compiler.forSite().compile(docsSite());
    Files.writeString(out.resolve("index.html"), "stale");

    SiteWriter.writeToDirectory(site, out);

    assertThat(out.resolve("index.html")).hasContent(site.pages().get("/"));
  }

  @Test
  @DisplayName("a relative output directory with dot segments resolves cleanly")
  void writesToARelativeDirectory() {
    var site = Compiler.forSite().compile(docsSite());
    var target = out.resolve("nested").resolve(".").resolve("site");
    var relative = Path.of("").toAbsolutePath().relativize(target);

    SiteWriter.writeToDirectory(site, relative);

    assertThat(target.resolve("index.html")).hasContent(site.pages().get("/"));
  }

  @Test
  @DisplayName(
      "files and directories left by an earlier build are deleted, top-level dot entries kept")
  void deletesStaleOutput() throws IOException {
    Files.createDirectories(out.resolve("old/nested"));
    Files.writeString(out.resolve("old/nested/page.html"), "stale");
    Files.writeString(out.resolve("removed.html"), "stale");
    Files.createDirectories(out.resolve(".git"));
    Files.writeString(out.resolve(".git/HEAD"), "ref");
    var site = Compiler.forSite().compile(docsSite());

    SiteWriter.writeToDirectory(site, out);

    assertThat(out.resolve("old")).doesNotExist();
    assertThat(out.resolve("removed.html")).doesNotExist();
    assertThat(out.resolve(".git/HEAD")).hasContent("ref");
    assertThat(out.resolve("index.html")).hasContent(site.pages().get("/"));
  }

  @Test
  @DisplayName("the not-found page is written as 404.html")
  void writesNotFoundPage() {
    var site =
        Compiler.forSite()
            .compile(
                docsSite().withNotFoundPage(PageDocument.of("Not found", main().text("Gone"))));

    SiteWriter.writeToDirectory(site, out);

    assertThat(out.resolve("404.html")).hasContent(site.notFoundPage());
  }

  @Test
  @DisplayName("the working directory, its parents and the home directory are refused")
  void refusesUnsafeDirectories() {
    var site = Compiler.forSite().compile(docsSite());
    var working = Path.of("").toAbsolutePath();

    for (var unsafe :
        List.of(working, working.getParent(), Path.of(System.getProperty("user.home")))) {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> SiteWriter.writeToDirectory(site, unsafe));
    }
  }
}
