package dev.nthings.tessera.site;

import static dev.nthings.tessera.site.SiteArtifacts.SEARCH_INDEX_PATH;
import static dev.nthings.tessera.site.SiteArtifacts.SITEMAP_PATH;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.SourceProvenance;
import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.render.CompilationDiagnostic;
import dev.nthings.tessera.render.CompilationException;
import dev.nthings.tessera.render.CompiledPage;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PreparedPage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

/// Compiles a [SiteDefinition] into a [CompiledSite].
public final class SiteCompiler {
  private SiteCompiler() {}

  /// Compiles every route of `site`. Pages are prepared concurrently, then links, fragments and
  /// transition keys are checked across all pages, the shared stylesheet is merged, and pages are
  /// painted concurrently. Render callbacks run on virtual threads, so they must not change shared
  /// application state. Exceptions thrown by render callbacks propagate unchanged.
  ///
  /// Throws [CompilationException] with the page diagnostics in route order. If every page
  /// compiled, it throws with the cross-route link and transition-key diagnostics instead. Throws
  /// IllegalArgumentException if two routes share a path, if an asset or generated file (such as
  /// `/sitemap.xml`) has the path of a route, if two files share a path, or if an external output
  /// base path is relative.
  public static CompiledSite compile(SiteDefinition site) {
    var routes = site.routes();
    var seenPaths = new LinkedHashSet<String>(routes.size());
    var preparedByRoute = new LinkedHashMap<String, PreparedPage>(routes.size());

    for (var route : routes) {
      if (!seenPaths.add(route.path())) {
        throw new IllegalArgumentException("Duplicate route path: " + route.path());
      }
    }
    var files = new LinkedHashMap<String, SiteAsset>();
    for (var asset : site.assets()) {
      addFile(files, seenPaths, asset);
    }

    var siteHead = site.linkSpeculation().headElements();
    var preparedPages = preparePages(site, siteHead);
    for (var i = 0; i < routes.size(); i++) {
      preparedByRoute.put(routes.get(i).path(), preparedPages.get(i));
    }

    var diagnostics = new ArrayList<CompilationDiagnostic>();
    SiteValidator.validateSiteLinks(preparedByRoute, files.keySet(), diagnostics);

    SiteValidator.validateViewTransitions(preparedByRoute, diagnostics);
    if (!diagnostics.isEmpty()) {
      throw new CompilationException(diagnostics);
    }

    // Merge route stylesheets in route order so output is deterministic.
    var sharedCollector = new StyleSheetCollector();
    for (var prepared : preparedPages) {
      prepared.contributeStylesTo(sharedCollector);
    }
    var sharedCssContent = sharedCollector.toCssStylesheet(site.tokens());

    var painted =
        parallelMap(
            preparedPages,
            prepared -> PageCompiler.paintPrepared(prepared, sharedCssContent, site.outputMode()));
    var compiledPages = new LinkedHashMap<String, CompiledPage>(routes.size());
    var generatedAssets = new LinkedHashMap<String, SiteAsset>();
    for (var i = 0; i < routes.size(); i++) {
      var page = painted.get(i);
      compiledPages.put(routes.get(i).path(), page);
      for (var asset : page.assets()) {
        generatedAssets.computeIfAbsent(
            asset.path(), path -> SiteAsset.text(path, asset.content()));
      }
    }
    generatedAssets.values().forEach(asset -> addFile(files, seenPaths, asset));
    // All pages share one output mode and stylesheet, so one path is enough.
    var sharedCssPath = painted.getFirst().externalCssPath();

    if (sharedCssPath != null) {
      addFile(files, seenPaths, SiteAsset.text(sharedCssPath, sharedCssContent));
    }
    var baseUrl = site.baseUrl();
    if (baseUrl != null) {
      addFile(
          files,
          seenPaths,
          SiteAsset.text(SITEMAP_PATH, SiteArtifacts.generateSitemapXml(baseUrl, routes)));
    }
    addFile(
        files,
        seenPaths,
        SiteAsset.text(
            SEARCH_INDEX_PATH, SiteArtifacts.generateSearchIndexJson(routes, preparedPages)));

    return new CompiledSite(compiledPages, sharedCssPath, sharedCssContent, files);
  }

  /// Adds `file`; rejects a path already used by a route or another file.
  private static void addFile(
      Map<String, SiteAsset> files, Set<String> routePaths, SiteAsset file) {
    if (routePaths.contains(file.path())) {
      throw new IllegalArgumentException("File path collides with route: " + file.path());
    }
    if (files.putIfAbsent(file.path(), file) != null) {
      throw new IllegalArgumentException("Duplicate file path: " + file.path());
    }
  }

  private sealed interface Preparation permits Prepared, Failed {}

  private record Prepared(PreparedPage page) implements Preparation {}

  private record Failed(List<CompilationDiagnostic> diagnostics) implements Preparation {}

  private static List<PreparedPage> preparePages(SiteDefinition site, List<Element> siteHead) {
    List<Preparation> preparations =
        parallelMap(
            site.routes(),
            route -> {
              try {
                return new Prepared(
                    PageCompiler.prepare(route.document(), site.tokens(), siteHead));
              } catch (CompilationException failure) {
                return new Failed(
                    failure.diagnostics().stream().map(d -> d.atRoute(route.path())).toList());
              }
            });
    var pages = new ArrayList<PreparedPage>(preparations.size());
    var diagnostics = new ArrayList<CompilationDiagnostic>();
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
