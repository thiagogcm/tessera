package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.compiler.SiteArtifacts.SEARCH_INDEX_PATH;
import static dev.nthings.tessera.compiler.SiteArtifacts.SITEMAP_PATH;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.SourceProvenance;
import dev.nthings.tessera.css.StyleSheetCollector;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

/// Site compilation behind [Compiler#compile(SiteDefinition)]: path checks, concurrent page
/// preparation, cross-route validation, stylesheet merge, concurrent painting and artifacts.
final class SiteCompiler {
  private SiteCompiler() {}

  static CompiledSite compile(SiteDefinition site) {
    var routes = site.routes();

    // Route and declared-file paths are known up front: reject conflicts before rendering.
    var pageTitles = new LinkedHashMap<String, String>(routes.size());
    var diagnostics = new ArrayList<Diagnostic>();
    for (var route : routes) {
      var title = route.document().title();
      if (pageTitles.putIfAbsent(route.path(), title) != null) {
        diagnostics.add(
            new Diagnostic(
                "duplicate-route",
                "Route path \"" + route.path() + "\" is declared more than once",
                "Give each route a distinct path, or remove the duplicate route.",
                Location.site(route.path(), title)));
      }
    }
    var files = new LinkedHashMap<String, Asset>();
    for (var asset : site.assets()) {
      addFile(files, pageTitles, asset, "File", diagnostics);
    }
    var baseUrl = site.baseUrl();
    // Generated later; a route or registered file must not take their paths.
    collides(files, pageTitles, SEARCH_INDEX_PATH, "Generated file", diagnostics);
    if (baseUrl != null) {
      collides(files, pageTitles, SITEMAP_PATH, "Generated file", diagnostics);
    }
    if (!diagnostics.isEmpty()) {
      throw new CompilationException(diagnostics);
    }

    var siteHead = site.linkSpeculation().headElements();
    var preparedPages = preparePages(site, siteHead);
    var preparedByRoute = new LinkedHashMap<String, PreparedPage>(routes.size());
    for (var i = 0; i < routes.size(); i++) {
      preparedByRoute.put(routes.get(i).path(), preparedPages.get(i));
    }

    SiteValidator.validateSiteLinks(preparedByRoute, files.keySet(), diagnostics);
    SiteValidator.validateViewTransitions(preparedByRoute, diagnostics);

    // Merge route stylesheets in route order so output is deterministic.
    var sharedCollector = new StyleSheetCollector();
    for (var prepared : preparedPages) {
      prepared.contributeStylesTo(sharedCollector);
    }
    var css = sharedCollector.toCssStylesheet(site.tokens());
    var outputMode = site.outputMode();
    var linkedFiles = new LinkedHashMap<String, Asset>();
    for (var prepared : preparedPages) {
      for (var asset : HtmlWriter.assets(css, outputMode, prepared.behaviors())) {
        linkedFiles.putIfAbsent(asset.path(), asset);
      }
    }
    for (var asset : linkedFiles.values()) {
      addFile(files, pageTitles, asset, "Generated file", diagnostics);
    }
    if (!diagnostics.isEmpty()) {
      throw new CompilationException(diagnostics);
    }

    var painted = parallelMap(preparedPages, page -> PageCompiler.paint(page, css, outputMode));
    var pages = new LinkedHashMap<String, String>(routes.size());
    for (var i = 0; i < routes.size(); i++) {
      pages.put(routes.get(i).path(), painted.get(i));
    }
    if (baseUrl != null) {
      files.put(
          SITEMAP_PATH,
          Asset.text(SITEMAP_PATH, SiteArtifacts.generateSitemapXml(baseUrl, routes)));
    }
    files.put(
        SEARCH_INDEX_PATH,
        Asset.text(
            SEARCH_INDEX_PATH, SiteArtifacts.generateSearchIndexJson(routes, preparedPages)));
    return new CompiledSite(pages, css, files);
  }

  /// Adds `file`, reporting a path already used by a route or another file.
  private static void addFile(
      SequencedMap<String, Asset> files,
      Map<String, String> pageTitles,
      Asset file,
      String kind,
      List<Diagnostic> diagnostics) {
    if (collides(files, pageTitles, file.path(), kind, diagnostics)) {
      return;
    }
    files.put(file.path(), file);
  }

  /// Reports `path` if a route or another file already uses it.
  private static boolean collides(
      Map<String, Asset> files,
      Map<String, String> pageTitles,
      String path,
      String kind,
      List<Diagnostic> diagnostics) {
    var title = pageTitles.get(path);
    if (title != null) {
      diagnostics.add(
          new Diagnostic(
              "file-path-collision",
              kind + " \"" + path + "\" has the same path as a route",
              "Publish the file or the route at a different path.",
              Location.site(path, title)));
      return true;
    }
    if (files.containsKey(path)) {
      diagnostics.add(
          new Diagnostic(
              "file-path-collision",
              kind + " \"" + path + "\" has the same path as another file",
              "Register each file at a distinct path.",
              Location.site(null, null)));
      return true;
    }
    return false;
  }

  private sealed interface Preparation permits Prepared, Failed {}

  private record Prepared(PreparedPage page) implements Preparation {}

  private record Failed(List<Diagnostic> diagnostics) implements Preparation {}

  private static List<PreparedPage> preparePages(SiteDefinition site, List<Element> siteHead) {
    List<Preparation> preparations =
        parallelMap(
            site.routes(),
            route -> {
              try {
                return new Prepared(PageCompiler.prepare(route.document(), siteHead));
              } catch (CompilationException failure) {
                return new Failed(
                    failure.diagnostics().stream().map(d -> d.atRoute(route.path())).toList());
              }
            });
    var pages = new ArrayList<PreparedPage>(preparations.size());
    var diagnostics = new ArrayList<Diagnostic>();
    for (var preparation : preparations) {
      switch (preparation) {
        case Prepared(var page) -> pages.add(page);
        case Failed(var errors) -> diagnostics.addAll(errors);
      }
    }
    if (!diagnostics.isEmpty()) {
      throw new CompilationException(diagnostics);
    }
    return pages;
  }

  /// Applies `fn` to every input on virtual threads, returning results in input order.
  private static <T, R> List<R> parallelMap(List<T> inputs, Function<T, R> fn) {
    var capture = SourceProvenance.current();
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures = new ArrayList<Future<R>>(inputs.size());
      for (var input : inputs) {
        futures.add(executor.submit(() -> SourceProvenance.with(capture, () -> fn.apply(input))));
      }
      var results = new ArrayList<R>(futures.size());
      for (var future : futures) {
        results.add(future.get());
      }
      return results;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Site compilation interrupted", e);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof RuntimeException unchecked) {
        throw unchecked;
      }
      throw new IllegalStateException("Site compilation failed", e.getCause());
    }
  }
}
