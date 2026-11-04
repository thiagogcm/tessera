package dev.nthings.tessera.site;

import dev.nthings.tessera.components.Toc;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.render.AccessibilityViolationException;
import dev.nthings.tessera.render.PageCompiler;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.owasp.encoder.Encode;

/// Multi-page static site compiler.
///
/// - Executes Phase 1 & Phase 2 concurrently across all routes on JDK 25 Virtual Threads.
/// - Deterministically merges each route's [StyleSheetCollector] in route declaration order into
///   a single shared site stylesheet.
/// - Verifies internal `<a href="/...">` route links and `#fragment` target IDs across the site
///   before running Phase 3 (`paint`).
public final class SiteCompiler {

  /// Compiled routes plus every other file the site publishes, keyed by URL path: registered
  /// assets, the shared stylesheet, `sitemap.xml` and `search-index.json`.
  public record CompiledSite(
      Map<String, PageCompiler.CompiledPage> pagesByRoute,
      @Nullable String sharedCssPath,
      String sharedCssContent,
      SequencedMap<String, SiteAsset> files) {

    public CompiledSite {
      files = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(files));
    }

    public @Nullable String sitemapXml() {
      return textFile(SITEMAP_PATH);
    }

    public @Nullable String searchIndexJson() {
      return textFile(SEARCH_INDEX_PATH);
    }

    private @Nullable String textFile(String path) {
      SiteAsset file = files.get(path);
      return file == null ? null : new String(file.contentView(), StandardCharsets.UTF_8);
    }
  }

  private static final String SITEMAP_PATH = "/sitemap.xml";
  private static final String SEARCH_INDEX_PATH = "/search-index.json";

  private SiteCompiler() {}

  public static CompiledSite compile(SiteDefinition site) {
    List<SiteRoute> routes = site.routes();
    var seenPaths = new LinkedHashSet<String>(routes.size());
    var preparedByRoute = new LinkedHashMap<String, PageCompiler.PreparedPage>(routes.size());

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
    List<PageCompiler.PreparedPage> preparedPages =
        parallelMap(routes, route -> PageCompiler.prepare(route.document(), siteHead));
    for (int i = 0; i < routes.size(); i++) {
      preparedByRoute.put(routes.get(i).path(), preparedPages.get(i));
    }

    // Verify cross-route links and #fragment targets
    validateSiteLinks(preparedByRoute, files.keySet());

    // Verify TransitionKey appearance across multiple routes
    validateViewTransitions(preparedByRoute);

    // Deterministically merge all route stylesheets in route order
    var sharedCollector = new StyleSheetCollector();
    for (var prepared : preparedPages) {
      sharedCollector.mergeFrom(prepared.styles());
    }
    String sharedCssContent = sharedCollector.toCssStylesheet(site.tokens());

    List<PageCompiler.CompiledPage> painted =
        parallelMap(
            preparedPages,
            prepared -> PageCompiler.paintPrepared(prepared, sharedCssContent, site.outputMode()));
    var compiledPages = new LinkedHashMap<String, PageCompiler.CompiledPage>(routes.size());
    String sharedCssPath = null;
    for (int i = 0; i < routes.size(); i++) {
      PageCompiler.CompiledPage compiled = painted.get(i);
      compiledPages.put(routes.get(i).path(), compiled);
      if (sharedCssPath == null && compiled.externalCssPath() != null) {
        sharedCssPath = compiled.externalCssPath();
      }
    }

    if (sharedCssPath != null) {
      addFile(files, seenPaths, SiteAsset.text(sharedCssPath, sharedCssContent));
    }
    String baseUrl = site.baseUrl();
    if (baseUrl != null && !baseUrl.isBlank()) {
      addFile(files, seenPaths, SiteAsset.text(SITEMAP_PATH, generateSitemapXml(baseUrl, routes)));
    }
    addFile(
        files,
        seenPaths,
        SiteAsset.text(SEARCH_INDEX_PATH, generateSearchIndexJson(routes, preparedByRoute)));

    return new CompiledSite(
        Collections.unmodifiableMap(compiledPages), sharedCssPath, sharedCssContent, files);
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

  /// Applies `fn` to every input on virtual threads, returning results in input order.
  private static <T, R> List<R> parallelMap(List<T> inputs, Function<T, R> fn) {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures = new ArrayList<Future<R>>(inputs.size());
      for (T input : inputs) {
        futures.add(executor.submit(() -> fn.apply(input)));
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
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException re) {
        throw re;
      }
      throw new IllegalStateException("Site compilation failed", cause);
    }
  }

  public static void writeToDirectory(CompiledSite compiledSite, Path outputDirectory) {
    try {
      Files.createDirectories(outputDirectory);
      for (var entry : compiledSite.pagesByRoute().entrySet()) {
        writeFile(
            outputDirectory,
            SiteRoute.outputPathFor(entry.getKey()),
            entry.getValue().html().getBytes(StandardCharsets.UTF_8));
      }
      for (var file : compiledSite.files().values()) {
        writeFile(outputDirectory, file.relativeOutputPath(), file.contentView());
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void writeFile(Path root, Path relative, byte[] content) throws IOException {
    Path target = root.resolve(relative).normalize();
    if (!target.startsWith(root.normalize())) {
      throw new IllegalArgumentException("Output escapes the site directory: " + relative);
    }
    Path parent = target.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.write(target, content);
  }

  private static void validateViewTransitions(
      Map<String, PageCompiler.PreparedPage> preparedByRoute) {
    var routesByKey = new LinkedHashMap<TransitionKey, Set<String>>();
    for (var entry : preparedByRoute.entrySet()) {
      String routePath = entry.getKey();
      for (var key : entry.getValue().transitionKeys()) {
        routesByKey.computeIfAbsent(key, _ -> new LinkedHashSet<>()).add(routePath);
      }
    }
    for (var entry : routesByKey.entrySet()) {
      var key = entry.getKey();
      var routePaths = entry.getValue();
      if (routePaths.size() < 2) {
        throw new AccessibilityViolationException(
            "TransitionKey '"
                + key.name()
                + "' declared on route '"
                + routePaths.iterator().next()
                + "' must appear on at least two distinct routes site-wide to enable cross-document morphing.");
      }
    }
  }

  private static String generateSitemapXml(String baseUrl, List<SiteRoute> routes) {
    var sb = new StringBuilder(1024);
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
    for (var route : routes) {
      String loc = baseUrl + ("/".equals(route.path()) ? "/" : route.path());
      sb.append("  <url>\n");
      sb.append("    <loc>").append(Encode.forXmlContent(loc)).append("</loc>\n");
      sb.append("  </url>\n");
    }
    sb.append("</urlset>\n");
    return sb.toString();
  }

  private static String generateSearchIndexJson(
      List<SiteRoute> routes, Map<String, PageCompiler.PreparedPage> preparedByRoute) {
    var sb = new StringBuilder(2048);
    sb.append("[\n");
    for (int i = 0; i < routes.size(); i++) {
      var route = routes.get(i);
      var prepared = preparedByRoute.get(route.path());
      String title = route.displayTitle();
      sb.append("  {\n");
      sb.append("    \"path\": \"").append(escapeJson(route.path())).append("\",\n");
      sb.append("    \"title\": \"").append(escapeJson(title)).append("\"");
      String routeDesc = route.description();
      if (routeDesc != null) {
        sb.append(",\n    \"description\": \"").append(escapeJson(routeDesc)).append("\"");
      }
      sb.append(",\n    \"headings\": [");
      if (prepared != null) {
        List<Toc.Item> tocs = Toc.entries(prepared.documentState());
        for (int j = 0; j < tocs.size(); j++) {
          var toc = tocs.get(j);
          if (j > 0) sb.append(", ");
          sb.append("{\"id\": \"")
              .append(escapeJson(toc.targetDomId()))
              .append("\", \"title\": \"")
              .append(escapeJson(toc.title()))
              .append("\", \"level\": ")
              .append(toc.level())
              .append("}");
        }
      }
      sb.append("]\n");
      sb.append("  }");
      if (i < routes.size() - 1) {
        sb.append(",");
      }
      sb.append("\n");
    }
    sb.append("]\n");
    return sb.toString();
  }

  private static String escapeJson(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
  }

  private static void validateSiteLinks(
      Map<String, PageCompiler.PreparedPage> preparedByRoute, Set<String> assetPaths) {
    for (var entry : preparedByRoute.entrySet()) {
      String sourceRoute = entry.getKey();
      PageCompiler.PreparedPage sourcePage = entry.getValue();

      for (String href : sourcePage.outgoingHrefs()) {
        // Same-page `#fragment` links are already validated by PageCompiler.prepare.
        if (href.startsWith("/") && !href.startsWith("//")) {
          int hashIdx = href.indexOf('#');
          String rawPath = hashIdx >= 0 ? href.substring(0, hashIdx) : href;
          String fragment = hashIdx >= 0 ? href.substring(hashIdx + 1) : "";
          if (assetPaths.contains(rawPath)) {
            continue;
          }
          String targetRoute =
              rawPath.isEmpty() ? sourceRoute : SiteRoute.normalizeRoutePath(rawPath);

          PageCompiler.PreparedPage targetPage = preparedByRoute.get(targetRoute);
          if (targetPage == null) {
            throw new AccessibilityViolationException(
                "Route \""
                    + sourceRoute
                    + "\" references unregistered internal route or asset \""
                    + href
                    + "\"");
          }
          if (!fragment.isEmpty() && !targetPage.registeredDomIds().contains(fragment)) {
            throw new AccessibilityViolationException(
                "Route \""
                    + sourceRoute
                    + "\" links to missing target anchor #"
                    + fragment
                    + " on route \""
                    + targetRoute
                    + "\"");
          }
        }
      }
    }
  }
}
