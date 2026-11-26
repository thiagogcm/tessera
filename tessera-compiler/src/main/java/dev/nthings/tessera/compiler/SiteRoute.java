package dev.nthings.tessera.compiler;

import com.google.errorprone.annotations.CheckReturnValue;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/// A route and the [PageDocument] published there. The path is normalized: it is trimmed, gains a
/// leading `/`, and loses one trailing `/` except on the root, so `guide/` and `/guide` name the
/// same route. A blank path throws IllegalArgumentException.
///
/// Create a route with [#of(String, PageDocument)] and optionally refine it with
/// [#withTitle(String)], which replaces the label in navigation and search without altering
/// `<title>`. Page metadata such as the description belongs to the [PageDocument]. Routes compare
/// by identity.
public final class SiteRoute {

  private final String path;
  private final PageDocument document;
  private final @Nullable String title;

  private SiteRoute(String path, PageDocument document, @Nullable String title) {
    this.path = normalizePath(path);
    this.document = document;
    this.title = title;
  }

  /// Creates a route publishing `document` at `path`.
  public static SiteRoute of(String path, PageDocument document) {
    return new SiteRoute(path, document, null);
  }

  /// Returns a copy of this route with `title` for navigation and search labels.
  @CheckReturnValue
  public SiteRoute withTitle(String title) {
    return new SiteRoute(path, document, title);
  }

  /// The normalized route path, such as `/guide`.
  public String path() {
    return path;
  }

  /// The document published at this route.
  public PageDocument document() {
    return document;
  }

  /// The navigation and search label set with [#withTitle(String)], or `null`; see
  /// [#displayTitle()].
  public @Nullable String title() {
    return title;
  }

  /// Normalizes a route path: trims it, adds a leading `/`, and removes one trailing `/` except on
  /// the root. Throws IllegalArgumentException if `rawPath` is blank.
  public static String normalizePath(String rawPath) {
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

  /// The browser URL of the published page. Directory routes end in `/`; routes ending in `.html`
  /// keep their path.
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

  /// The output file of a normalized route, relative to the output directory, such as
  /// `guide/index.html`.
  static Path outputPathFor(String routePath) {
    if ("/".equals(routePath)) {
      return Path.of("index.html");
    }
    var stripped = routePath.substring(1);
    if (stripped.endsWith(".html")) {
      return Path.of(stripped);
    }
    return Path.of(stripped, "index.html");
  }

  /// The URL path of the output file of a normalized route, such as `/guide/index.html`.
  static String outputUrlPathFor(String routePath) {
    return "/" + outputPathFor(routePath).toString().replace('\\', '/');
  }

  @Override
  public String toString() {
    return "SiteRoute[path=" + path + ", title=" + displayTitle() + "]";
  }
}
