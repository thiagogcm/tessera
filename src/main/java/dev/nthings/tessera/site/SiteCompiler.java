package dev.nthings.tessera.site;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.jspecify.annotations.Nullable;

/**
 * Multi-page static site compiler.
 *
 * <ul>
 *   <li>Executes Phase 1 & Phase 2 concurrently across all routes on JDK 25 Virtual Threads.
 *   <li>Deterministically merges each route's {@link StyleSheetCollector} in route declaration
 *       order into a single shared site stylesheet.
 *   <li>Verifies internal {@code <a href="/...">} route links and {@code #fragment} target IDs
 *       across the site before running Phase 3 ({@code paint}).
 * </ul>
 */
public final class SiteCompiler {

  public record CompiledSite(
      Map<String, PageCompiler.CompiledPage> pagesByRoute,
      @Nullable String sharedCssPath,
      String sharedCssContent) {}

  private SiteCompiler() {}

  public static CompiledSite compile(SiteDefinition site) {
    List<SiteRoute> routes = site.routes();
    var seenPaths = new LinkedHashSet<String>(routes.size());
    var preparedByRoute = new LinkedHashMap<String, PageCompiler.PreparedPage>(routes.size());

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures = new ArrayList<Future<PageCompiler.PreparedPage>>(routes.size());
      for (var route : routes) {
        if (!seenPaths.add(route.path())) {
          throw new IllegalArgumentException("Duplicate route path: " + route.path());
        }
        futures.add(executor.submit(() -> PageCompiler.prepare(route.document())));
      }

      for (int i = 0; i < routes.size(); i++) {
        preparedByRoute.put(routes.get(i).path(), futures.get(i).get());
      }
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

    // Verify cross-route links and #fragment targets
    validateSiteLinks(preparedByRoute);

    // Deterministically merge all route stylesheets in route order
    var sharedCollector = new StyleSheetCollector();
    for (var prepared : preparedByRoute.values()) {
      sharedCollector.mergeFrom(prepared.styles());
    }
    String sharedCssContent = sharedCollector.toCssStylesheet(site.tokens());

    var compiledPages = new LinkedHashMap<String, PageCompiler.CompiledPage>(routes.size());
    String sharedCssPath = null;
    for (var entry : preparedByRoute.entrySet()) {
      PageCompiler.CompiledPage compiled =
          PageCompiler.paintPrepared(entry.getValue(), sharedCssContent, site.outputMode());
      compiledPages.put(entry.getKey(), compiled);
      if (sharedCssPath == null && compiled.externalCssPath() != null) {
        sharedCssPath = compiled.externalCssPath();
      }
    }

    return new CompiledSite(
        Collections.unmodifiableMap(compiledPages), sharedCssPath, sharedCssContent);
  }

  public static void writeToDirectory(CompiledSite compiledSite, Path outputDirectory) {
    try {
      Files.createDirectories(outputDirectory);
      for (var entry : compiledSite.pagesByRoute().entrySet()) {
        var route = new SiteRoute(entry.getKey(), placeholderDocument());
        Path pageFile = outputDirectory.resolve(route.toRelativeOutputPath());
        Path parent = pageFile.getParent();
        if (parent != null) {
          Files.createDirectories(parent);
        }
        Files.writeString(pageFile, entry.getValue().html(), StandardCharsets.UTF_8);
      }

      String cssPath = compiledSite.sharedCssPath();
      if (cssPath != null) {
        String relCss = cssPath.startsWith("/") ? cssPath.substring(1) : cssPath;
        Path cssFile = outputDirectory.resolve(relCss);
        Path parent = cssFile.getParent();
        if (parent != null) {
          Files.createDirectories(parent);
        }
        Files.writeString(cssFile, compiledSite.sharedCssContent(), StandardCharsets.UTF_8);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static dev.nthings.tessera.render.PageDocument placeholderDocument() {
    return new dev.nthings.tessera.render.PageDocument(
        "Placeholder", dev.nthings.tessera.core.Elements.empty());
  }

  private static void validateSiteLinks(Map<String, PageCompiler.PreparedPage> preparedByRoute) {
    for (var entry : preparedByRoute.entrySet()) {
      String sourceRoute = entry.getKey();
      PageCompiler.PreparedPage sourcePage = entry.getValue();

      for (String href : sourcePage.outgoingHrefs()) {
        if (href.startsWith("#")) {
          String fragment = href.substring(1);
          if (!fragment.isEmpty() && !sourcePage.registeredDomIds().contains(fragment)) {
            throw new AccessibilityViolationException(
                "Route \""
                    + sourceRoute
                    + "\" contains broken same-page fragment link href=\""
                    + href
                    + "\"");
          }
        } else if (href.startsWith("/") && !href.startsWith("//")) {
          int hashIdx = href.indexOf('#');
          String rawPath = hashIdx >= 0 ? href.substring(0, hashIdx) : href;
          String fragment = hashIdx >= 0 ? href.substring(hashIdx + 1) : "";
          String targetRoute =
              rawPath.isEmpty() ? sourceRoute : SiteRoute.normalizeRoutePath(rawPath);

          PageCompiler.PreparedPage targetPage = preparedByRoute.get(targetRoute);
          if (targetPage == null) {
            throw new AccessibilityViolationException(
                "Route \""
                    + sourceRoute
                    + "\" links to unregistered internal route href=\""
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
