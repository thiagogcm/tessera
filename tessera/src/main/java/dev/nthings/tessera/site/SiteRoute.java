package dev.nthings.tessera.site;

import dev.nthings.tessera.render.PageDocument;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/// A route and the [PageDocument] published there. The path is normalized by
/// [#normalizeRoutePath(String)], so `guide/` and `/guide` name the same route. `title` replaces
/// the name shown in navigation and search, but not the page's `<title>`, which comes from the
/// document. `description` feeds `search-index.json` and the search palette.
public record SiteRoute(
    String path, PageDocument document, @Nullable String title, @Nullable String description) {

  public SiteRoute {
    path = normalizeRoutePath(path);
  }

  public SiteRoute(String path, PageDocument document) {
    this(path, document, null, null);
  }

  public SiteRoute(String path, PageDocument document, String title) {
    this(path, document, title, null);
  }

  /// Normalizes a route path: trims it, adds a leading `/`, and removes one trailing `/` except on
  /// the root. Throws IllegalArgumentException if `rawPath` is blank.
  public static String normalizeRoutePath(String rawPath) {
    if (rawPath.isBlank()) {
      throw new IllegalArgumentException("Route path cannot be blank");
    }
    var p = rawPath.trim();
    if (!p.startsWith("/")) {
      p = "/" + p;
    }
    if (p.length() > 1 && p.endsWith("/")) {
      p = p.substring(0, p.length() - 1);
    }
    return p;
  }

  /// The title shown in navigation and search: `title` if set, otherwise the document title.
  public String displayTitle() {
    return title != null ? title : document.title();
  }

  /// The browser URL of the published page. Directory routes end in `/`; routes ending in
  /// `.html` keep their path.
  public URI documentUri() {
    return documentUriFor(path);
  }

  static URI documentUriFor(String routePath) {
    var documentPath =
        routePath.endsWith(".html") || routePath.endsWith("/") ? routePath : routePath + "/";
    try {
      return new URI(null, null, documentPath, null);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid route path: " + routePath, e);
    }
  }

  /// Compiler-facing: returns the output file of a normalized route, relative to the output
  /// directory, such as `guide/index.html`.
  public static Path outputPathFor(String routePath) {
    if ("/".equals(routePath)) {
      return Path.of("index.html");
    }
    var stripped = routePath.substring(1);
    if (stripped.endsWith(".html")) {
      return Path.of(stripped);
    }
    return Path.of(stripped, "index.html");
  }
}
