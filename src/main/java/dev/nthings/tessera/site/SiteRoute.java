package dev.nthings.tessera.site;

import dev.nthings.tessera.render.PageDocument;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/** Represents a single route and its associated {@link PageDocument} in a multi-page site. */
public record SiteRoute(
    String path,
    PageDocument document,
    @Nullable String title,
    @Nullable String description,
    int navOrder) {

  public SiteRoute {
    path = normalizeRoutePath(path);
  }

  public SiteRoute(String path, PageDocument document) {
    this(path, document, null, null, 0);
  }

  public SiteRoute(String path, PageDocument document, String title) {
    this(path, document, title, null, 0);
  }

  public SiteRoute(String path, PageDocument document, String title, int navOrder) {
    this(path, document, title, null, navOrder);
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

  public Path toRelativeOutputPath() {
    if ("/".equals(path)) {
      return Path.of("index.html");
    }
    String stripped = path.substring(1);
    if (stripped.endsWith(".html")) {
      return Path.of(stripped);
    }
    return Path.of(stripped, "index.html");
  }
}
