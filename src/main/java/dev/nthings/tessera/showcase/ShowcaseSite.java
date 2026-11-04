package dev.nthings.tessera.showcase;

import dev.nthings.tessera.render.PageDocument;
import dev.nthings.tessera.site.SiteCompiler;
import dev.nthings.tessera.site.SiteDefinition;
import dev.nthings.tessera.site.SitePreviewServer;
import dev.nthings.tessera.site.SiteRoute;
import java.nio.file.Path;
import java.util.List;

/// Showcase site entry point: the page registry drives routes, navigation, search, and pagers.
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
    var routes =
        PAGES.stream()
            .map(
                page ->
                    new SiteRoute(
                        page.path(),
                        new PageDocument("Tessera — " + page.navLabel(), new Shell(page)),
                        page.title(),
                        page.summary()))
            .toList();
    return SiteDefinition.builder()
        .routes(routes)
        .baseUrl("http://localhost:8188")
        .assets(Art.assets())
        .build();
  }

  public static void main(String[] args) throws InterruptedException {
    var outputDir = Path.of("target/site");
    var serve = false;
    var port = 8080;

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
        default ->
            throw new IllegalArgumentException(
                "Unknown option " + args[i] + "; usage: [--output <dir>] [--serve [port]]");
      }
    }

    var compiled = SiteCompiler.compile(createDefinition());
    SiteCompiler.writeToDirectory(compiled, outputDir);
    System.out.println(
        "Compiled "
            + compiled.pagesByRoute().size()
            + " routes + shared stylesheet ("
            + compiled.sharedCssPath()
            + ") to "
            + outputDir.toAbsolutePath());

    if (serve) {
      try (var server = SitePreviewServer.start(compiled, port)) {
        System.out.println("Serving Tessera showcase at " + server.baseUrl() + " (Ctrl+C to stop)");
        Thread.currentThread().join();
      }
    }
  }
}
