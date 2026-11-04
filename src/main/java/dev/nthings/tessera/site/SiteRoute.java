package dev.nthings.tessera.site;

import dev.nthings.tessera.render.PageDocument;
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
    String p = rawPath.trim();
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

  /// Maps a normalized route path to its output file relative to the site root.
  public static Path outputPathFor(String routePath) {
    if ("/".equals(routePath)) {
      return Path.of("index.html");
    }
    String stripped = routePath.substring(1);
    if (stripped.endsWith(".html")) {
      return Path.of(stripped);
    }
    return Path.of(stripped, "index.html");
  }
}
