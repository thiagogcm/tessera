package dev.nthings.tessera;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.main;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.nthings.tessera.components.Card;
import dev.nthings.tessera.components.DataTable;
import dev.nthings.tessera.components.DataTable.Column;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.render.AccessibilityViolationException;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PageDocument;
import dev.nthings.tessera.site.ShowcaseSite;
import dev.nthings.tessera.site.SiteCompiler;
import dev.nthings.tessera.site.SiteDefinition;
import dev.nthings.tessera.site.SitePreviewServer;
import dev.nthings.tessera.site.SiteRoute;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SiteCompilerAndScopeStyleTest {

  @Test
  void deduplicatesIdenticalScopeStylesAndQualifiesDistinctParameterizedViews() {
    var card1 = Card.create("one").title("First").bodyText("Body 1");
    var card2 = Card.create("two").title("Second").bodyText("Body 2");

    var table2Col =
        new DataTable<>(
            "two-col",
            "Two Columns",
            List.of(Column.rowHeader("Key", s -> main().text(s)), Column.text("Value", s -> s)),
            List.of("row-a"));

    var table3Col =
        new DataTable<>(
            "three-col",
            "Three Columns",
            List.of(
                Column.rowHeader("Key", s -> main().text(s)),
                Column.text("Mid", s -> s),
                Column.numeric("Count", _ -> "1")),
            List.of("row-b"));

    var compiled =
        PageCompiler.compile(
            new PageDocument(
                "Scope Deduplication", main().children(card1, card2, table2Col, table3Col)));

    String css = compiled.cssContent();
    // Only a single @scope ([data-view="card"]) block should exist despite 2 Card instances
    int firstCardScope = css.indexOf("@scope ([data-view=\"card\"]) to ([data-view])");
    int lastCardScope = css.lastIndexOf("@scope ([data-view=\"card\"]) to ([data-view])");
    assertThat(firstCardScope).isGreaterThan(0).isEqualTo(lastCardScope);

    // Parameterized DataTables with different columns get distinct data-view keys
    var doc = Jsoup.parse(compiled.html());
    var firstTableWrapper = doc.selectFirst("[data-view-id=datatable-two-col]");
    var secondTableWrapper = doc.selectFirst("[data-view-id=datatable-three-col]");
    assertThat(firstTableWrapper).isNotNull();
    assertThat(secondTableWrapper).isNotNull();
    assertThat(firstTableWrapper.attr("data-view")).isEqualTo("data-table");
    assertThat(secondTableWrapper.attr("data-view")).startsWith("data-table-");
  }

  @Test
  void compilesMultiPageShowcaseSiteWritesToDiskAndServesOverHttp(@TempDir Path tempDir)
      throws Exception {
    SiteDefinition definition = ShowcaseSite.createDefinition();
    SiteCompiler.CompiledSite compiledSite = SiteCompiler.compile(definition);

    assertThat(compiledSite.pagesByRoute()).containsOnlyKeys("/", "/telemetry", "/typography");
    assertThat(compiledSite.sharedCssPath()).startsWith("/assets/styles-").endsWith(".css");
    assertThat(compiledSite.sharedCssContent())
        .contains(
            "@media (prefers-reduced-motion: no-preference){@view-transition{navigation:auto}")
        .contains(
            "::view-transition-old(root),::view-transition-new(root){animation-duration:0.2s}")
        .contains("@scope ([data-view=\"card\"]) to ([data-view])")
        .contains("@scope ([data-view=\"data-table\"]) to ([data-view])")
        .contains("@scope ([data-view=\"step-list\"]) to ([data-view])");

    // Verify disk output
    SiteCompiler.writeToDirectory(compiledSite, tempDir);
    assertThat(Files.exists(tempDir.resolve("index.html"))).isTrue();
    assertThat(Files.exists(tempDir.resolve("telemetry/index.html"))).isTrue();
    assertThat(Files.exists(tempDir.resolve("typography/index.html"))).isTrue();
    String relCss = compiledSite.sharedCssPath().substring(1);
    assertThat(Files.exists(tempDir.resolve(relCss))).isTrue();

    // Verify live HTTP preview server
    try (var server = SitePreviewServer.start(compiledSite, 0);
        var client = HttpClient.newHttpClient()) {
      String base = server.baseUrl();

      var homeRes =
          client.send(
              HttpRequest.newBuilder(URI.create(base + "/")).GET().build(),
              HttpResponse.BodyHandlers.ofString());
      assertThat(homeRes.statusCode()).isEqualTo(200);
      assertThat(homeRes.body()).contains("Declarative HTML &amp; CSS View Engine for JDK 25");

      var telemetryRes =
          client.send(
              HttpRequest.newBuilder(URI.create(base + "/telemetry")).GET().build(),
              HttpResponse.BodyHandlers.ofString());
      assertThat(telemetryRes.statusCode()).isEqualTo(200);
      assertThat(telemetryRes.body()).contains("Regional Service SLO Telemetry");

      var cssRes =
          client.send(
              HttpRequest.newBuilder(URI.create(base + compiledSite.sharedCssPath())).GET().build(),
              HttpResponse.BodyHandlers.ofString());
      assertThat(cssRes.statusCode()).isEqualTo(200);
      assertThat(cssRes.headers().firstValue("Content-Type")).hasValue("text/css; charset=utf-8");

      var notFoundRes =
          client.send(
              HttpRequest.newBuilder(URI.create(base + "/missing")).GET().build(),
              HttpResponse.BodyHandlers.ofString());
      assertThat(notFoundRes.statusCode()).isEqualTo(404);
    }
  }

  @Test
  void rejectsBrokenCrossRouteLinksAndMissingAnchorsAtCompileTime() {
    var brokenRouteSite =
        new SiteDefinition(
            new SiteRoute(
                "/",
                new PageDocument(
                    "Home",
                    main().child(a("/does-not-exist").px(CssLength.rem(1)).text("Broken link")))));

    assertThatThrownBy(() -> SiteCompiler.compile(brokenRouteSite))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("unregistered internal route href=\"/does-not-exist\"");

    var brokenAnchorSite =
        new SiteDefinition(
            new SiteRoute(
                "/",
                new PageDocument(
                    "Home", main().child(a("/docs#missing-section").text("Jump to section")))),
            new SiteRoute(
                "/docs", new PageDocument("Docs", main().id("intro").text("Introduction"))));

    assertThatThrownBy(() -> SiteCompiler.compile(brokenAnchorSite))
        .isInstanceOf(AccessibilityViolationException.class)
        .hasMessageContaining("missing target anchor #missing-section on route \"/docs\"");
  }
}
