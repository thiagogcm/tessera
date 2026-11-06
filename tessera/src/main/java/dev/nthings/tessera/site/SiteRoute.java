package dev.nthings.tessera.site;

import dev.nthings.tessera.render.PageDocument;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/// Represents a single route and its associated [PageDocument] in a multi-page site.
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

  /// Route title if set, otherwise the document title.
  public String displayTitle() {
    return title != null ? title : document.title();
  }

  /// Browser URL of the published page. Directory routes end in `/`; HTML files keep their path.
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

  /// Maps a normalized route path to its output file relative to the site root.
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
