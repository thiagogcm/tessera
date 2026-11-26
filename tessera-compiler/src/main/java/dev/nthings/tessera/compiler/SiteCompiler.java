package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.compiler.SiteArtifacts.SEARCH_INDEX_PATH;
import static dev.nthings.tessera.compiler.SiteArtifacts.SITEMAP_PATH;

import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.document.PageAddress;
import dev.nthings.tessera.document.PageScript;
import dev.nthings.tessera.internal.authoring.SourceProvenance;
import dev.nthings.tessera.internal.css.StyleSheet;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;

/// Site compilation behind [Compiler#compile(SiteDefinition)]: path checks, concurrent page
/// preparation, cross-route validation, stylesheet merge, concurrent painting and artifacts.
final class SiteCompiler {
  private SiteCompiler() {}

  static CompiledSite compile(SiteDefinition site, Theme theme, OutputMode output) {
    var routes = site.routes();

    // Route paths and declared-file paths are known up front: reject them before rendering.
    var diagnostics = new ArrayList<Diagnostic>();
    var routePaths = new RoutePaths();
    for (var route : routes) {
      routePaths.add(route, diagnostics);
    }
    var files = new LinkedHashMap<String, Asset>();
    for (var asset : site.assets()) {
      addFile(files, routePaths, asset, "File", diagnostics);
    }
    var baseUrl = site.baseUrl();
    // Generated later; a route or registered file must not take their paths.
    collides(files, routePaths, SEARCH_INDEX_PATH, "Generated file", diagnostics);
    if (baseUrl != null) {
      collides(files, routePaths, SITEMAP_PATH, "Generated file", diagnostics);
    }
    if (!diagnostics.isEmpty()) {
      throw new CompilationException(diagnostics);
    }

    var speculation = site.linkSpeculation();
    var addresses = routes.stream().map(route -> address(route, baseUrl)).toList();
    var preparedPages = preparePages(routes, addresses, new BundlePublication(output));
    var preparedByRoute = new LinkedHashMap<String, PreparedPage>(routes.size());
    for (var i = 0; i < routes.size(); i++) {
      preparedByRoute.put(routes.get(i).path(), preparedPages.get(i));
    }

    // Merge route stylesheets in route order so output is deterministic.
    var sharedStyles = new StyleSheet();
    for (var prepared : preparedPages) {
      prepared.contributeStylesTo(sharedStyles);
    }
    var css = sharedStyles.render(theme);
    var behaviors = new LinkedHashSet<PageScript>();
    for (var prepared : preparedPages) {
      behaviors.addAll(prepared.scripts());
    }
    // One stylesheet and one file per distinct module, hashed and encoded once for every page.
    var resources = new HtmlWriter.Resources(css, output, behaviors);
    var externalRules =
        output instanceof OutputMode.External(var externalBase)
                && preparedPages.stream().anyMatch(page -> page.bundleBootstrap() != null)
            ? speculation.externalRules(externalBase)
            : null;
    var linkedFiles = new LinkedHashMap<String, Asset>();
    for (var asset : resources.assets()) {
      linkedFiles.putIfAbsent(asset.path(), asset);
    }
    for (var page : preparedPages) {
      for (var asset : page.bundleAssets()) {
        linkedFiles.putIfAbsent(asset.path(), asset);
      }
    }
    if (externalRules != null) {
      linkedFiles.putIfAbsent(externalRules.path(), externalRules);
    }
    // Links may point at any published file, including the generated ones.
    var publishedFiles = new LinkedHashSet<>(files.keySet());
    publishedFiles.addAll(linkedFiles.keySet());
    publishedFiles.add(SEARCH_INDEX_PATH);
    if (baseUrl != null) {
      publishedFiles.add(SITEMAP_PATH);
    }
    SiteValidator.validateSiteLinks(preparedByRoute, publishedFiles, diagnostics);
    SiteValidator.validateViewTransitions(preparedByRoute, diagnostics);
    for (var asset : linkedFiles.values()) {
      addFile(files, routePaths, asset, "Generated file", diagnostics);
    }
    if (!diagnostics.isEmpty()) {
      throw new CompilationException(diagnostics);
    }

    var emitted =
        parallelMap(
            IntStream.range(0, routes.size()).boxed().toList(),
            i -> {
              var page = preparedPages.get(i);
              return PageCompiler.emit(
                  page,
                  resources,
                  page.responseHeaders(output).isEmpty() ? speculation.inlineRules() : null,
                  canonicalUrl(routes.get(i), addresses.get(i)));
            });
    var pages = new LinkedHashMap<String, String>(routes.size());
    for (var i = 0; i < routes.size(); i++) {
      pages.put(routes.get(i).path(), emitted.get(i));
    }
    if (baseUrl != null) {
      files.put(
          SITEMAP_PATH, Asset.text(SITEMAP_PATH, SiteArtifacts.generateSitemapXml(addresses)));
    }
    files.put(
        SEARCH_INDEX_PATH,
        Asset.text(
            SEARCH_INDEX_PATH, SiteArtifacts.generateSearchIndexJson(routes, preparedPages)));
    var pageHeaders = new LinkedHashMap<String, Map<String, String>>();
    preparedByRoute.forEach((route, page) -> {
      var headers = page.responseHeaders(output);
      if (!headers.isEmpty()) {
        var published = new LinkedHashMap<>(headers);
        if (externalRules != null) {
          published.put("Speculation-Rules", '"' + externalRules.path() + '"');
        }
        pageHeaders.put(route, published);
      }
    });
    return new CompiledSite(pages, css, files, pageHeaders);
  }

  /// The URL paths and output files taken by routes.
  private static final class RoutePaths {
    private final Map<String, String> titles = new LinkedHashMap<>();
    private final Map<String, String> routesByOutput = new LinkedHashMap<>();

    /// Takes `route`'s path and output file, reporting a route that already holds either.
    void add(SiteRoute route, List<Diagnostic> diagnostics) {
      var path = route.path();
      var title = route.document().title();
      if (titles.putIfAbsent(path, title) != null) {
        diagnostics.add(
            new Diagnostic(
                "duplicate-route",
                "Route path \"" + path + "\" is declared more than once",
                "Give each route a distinct path, or remove the duplicate route.",
                CompilationState.siteLocation(path, title)));
        return;
      }
      var output = SiteRoute.outputUrlPathFor(path);
      var other = routesByOutput.putIfAbsent(output, path);
      if (other != null) {
        diagnostics.add(
            new Diagnostic(
                "file-path-collision",
                "Route \"" + path + "\" is written to " + output + ", like route \"" + other + "\"",
                "Publish one of the routes at a different path.",
                CompilationState.siteLocation(path, title)));
      }
    }
  }

  /// Adds `file`, reporting a path already used by a route or another file.
  private static void addFile(
      SequencedMap<String, Asset> files,
      RoutePaths routes,
      Asset file,
      String kind,
      List<Diagnostic> diagnostics) {
    if (collides(files, routes, file.path(), kind, diagnostics)) {
      return;
    }
    files.put(file.path(), file);
  }

  /// Reports `path` if a route or another file already uses it, or if a route's page is written to
  /// the same output file.
  private static boolean collides(
      Map<String, Asset> files,
      RoutePaths routes,
      String path,
      String kind,
      List<Diagnostic> diagnostics) {
    var title = routes.titles.get(path);
    if (title != null) {
      diagnostics.add(
          new Diagnostic(
              "file-path-collision",
              kind + " \"" + path + "\" has the same path as a route",
              "Publish the file or the route at a different path.",
              CompilationState.siteLocation(path, title)));
      return true;
    }
    var page = routes.routesByOutput.get(path);
    if (page != null) {
      diagnostics.add(
          new Diagnostic(
              "file-path-collision",
              kind + " \"" + path + "\" is written over the page of route \"" + page + "\"",
              "Publish the file or the route at a different path.",
              CompilationState.siteLocation(page, routes.titles.get(page))));
      return true;
    }
    if (files.containsKey(path)) {
      diagnostics.add(
          new Diagnostic(
              "file-path-collision",
              kind + " \"" + path + "\" has the same path as another file",
              "Register each file at a distinct path.",
              Location.ROOT));
      return true;
    }
    return false;
  }

  private sealed interface Preparation permits Prepared, Failed {}

  private record Prepared(PreparedPage page) implements Preparation {}

  private record Failed(List<Diagnostic> diagnostics) implements Preparation {}

  private static List<PreparedPage> preparePages(
      List<SiteRoute> routes, List<PageAddress> addresses, BundlePublication bundles) {
    List<Preparation> preparations =
        parallelMap(
            IntStream.range(0, routes.size()).boxed().toList(),
            i -> {
              var route = routes.get(i);
              try {
                return new Prepared(
                    PageCompiler.prepare(route.document(), addresses.get(i), bundles));
              } catch (CompilationException failure) {
                return new Failed(
                    failure.diagnostics().stream()
                        .map(d -> CompilationState.atRoute(d, route.path()))
                        .toList());
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

  /// Where `route` is published, absolute under `baseUrl` when the site has one.
  private static PageAddress address(SiteRoute route, @Nullable URI baseUrl) {
    var address = PageAddress.of(route.path());
    return baseUrl != null ? address.withBaseUrl(baseUrl) : address;
  }

  /// The document's own canonical URL, else the page's absolute URL, else none.
  private static @Nullable URI canonicalUrl(SiteRoute route, PageAddress address) {
    var own = route.document().canonicalUrl();
    return own != null ? own : address.absoluteUrl().orElse(null);
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
      switch (e.getCause()) {
        case RuntimeException unchecked -> throw unchecked;
        case Error error -> throw error;
        case null, default -> {}
      }
      throw new IllegalStateException("Site compilation failed", e.getCause());
    }
  }
}
