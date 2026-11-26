package dev.nthings.tessera.showcase;

import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.compiler.SiteDefinition;
import dev.nthings.tessera.compiler.SitePreviewServer;
import dev.nthings.tessera.compiler.SiteRoute;
import dev.nthings.tessera.compiler.SiteWriter;
import dev.nthings.tessera.compiler.SourceTracking;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/// Entry point; the page registry drives routes, navigation, search and pagers.
public final class ShowcaseSite {

  private static final List<Page> PAGES =
      List.of(
          OverviewPage.page(),
          LayoutPage.page(),
          NavigationPage.page(),
          OverlaysPage.page(),
          FormsPage.page(),
          DataPage.page(),
          DocsPage.page(),
          MotionPage.page());

  private ShowcaseSite() {}

  static List<Page> pages() {
    return PAGES;
  }

  public static SiteDefinition createDefinition() {
    var routes = new ArrayList<SiteRoute>();
    for (var page : PAGES) {
      routes.add(
          SiteRoute.of(
                  page.path(),
                  PageDocument.of("Tessera — " + page.navLabel(), new Shell(page))
                      .withDescription(page.summary()))
              .withTitle(page.title()));
    }
    routes.addAll(Catalog.routes());
    return SiteDefinition.builder()
        .routes(routes)
        .baseUrl(URI.create("http://localhost:8188"))
        .assets(Art.assets())
        .build();
  }

  /// The element tree of one route, with the Java line that built each element.
  static String explain(SiteDefinition definition, String path) {
    var route =
        definition.route(path).orElseThrow(() -> new IllegalArgumentException("No route " + path));
    return SourceTracking.enabled(() -> Compiler.explain(route.document())).toText();
  }

  public static void main(String[] args) throws InterruptedException {
    var outputDir = Path.of("target/site");
    var serve = false;
    var port = 8080;
    var sources = false;
    String explain = null;

    for (var i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--output" -> {
          if (++i == args.length) {
            throw new IllegalArgumentException("--output requires a directory");
          }
          outputDir = Path.of(args[i]);
        }
        case "--serve" -> {
          serve = true;
          if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
            port = Integer.parseInt(args[++i]);
          }
        }
        case "--sources" -> sources = true;
        case "--explain" -> {
          if (++i == args.length) {
            throw new IllegalArgumentException("--explain requires a route path");
          }
          explain = args[i];
        }
        default ->
            throw new IllegalArgumentException(
                "Unknown option "
                    + args[i]
                    + "; usage: [--output <dir>] [--serve [port]] [--sources] [--explain <route>]");
      }
    }

    var tracking = sources || explain != null;
    var definition = SourceTracking.enabledIf(tracking, ShowcaseSite::createDefinition);
    if (explain != null) {
      System.out.print(explain(definition, explain));
      return;
    }

    var compiled = SourceTracking.enabledIf(tracking, () -> Compiler.forSite().compile(definition));
    SiteWriter.writeToDirectory(compiled, outputDir);
    System.out.println(
        "Compiled "
            + compiled.pages().size()
            + " routes and "
            + compiled.files().size()
            + " files to "
            + outputDir.toAbsolutePath());

    if (serve) {
      try (var server = SitePreviewServer.start(compiled, port)) {
        System.out.println("Serving Tessera showcase at " + server.baseUrl() + " (Ctrl+C to stop)");
        Thread.currentThread().join();
      }
    }
  }
}
