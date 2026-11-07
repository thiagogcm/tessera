package dev.nthings.tessera.site;

import static dev.nthings.tessera.site.SiteArtifacts.SEARCH_INDEX_PATH;
import static dev.nthings.tessera.site.SiteArtifacts.SITEMAP_PATH;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.SourceTracking;
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

/// Coordinates concurrent page compilation, site validation, shared CSS, and artifacts.
public final class SiteCompiler {
  private SiteCompiler() {}

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

    // Verify cross-route links and #fragment targets
    var diagnostics = new ArrayList<CompilationDiagnostic>();
    SiteValidator.validateSiteLinks(preparedByRoute, files.keySet(), diagnostics);

    // Verify TransitionKey appearance across multiple routes
    SiteValidator.validateViewTransitions(preparedByRoute, diagnostics);
    if (!diagnostics.isEmpty()) {
      throw new CompilationException(diagnostics);
    }

    // Deterministically merge all route stylesheets in route order
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
    // Every page is painted with the same output mode and stylesheet, so they share one path.
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

  /// Registers `file`, rejecting a path already taken by a route or another file.
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
    var tracking = SourceTracking.active();
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures = new ArrayList<Future<R>>(inputs.size());
      for (var input : inputs) {
        futures.add(executor.submit(() -> SourceTracking.with(tracking, () -> fn.apply(input))));
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
