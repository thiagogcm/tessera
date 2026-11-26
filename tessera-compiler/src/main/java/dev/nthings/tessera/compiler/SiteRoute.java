package dev.nthings.tessera.compiler;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.document.PageAddress;
import java.net.URI;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/// A route and the [PageDocument] published there. The path is normalized: it is trimmed, gains a
/// leading `/`, and loses one trailing `/` except on the root, so `guide/` and `/guide` name the
/// same route. A blank path throws IllegalArgumentException.
///
/// Create a route with [#of(String, PageDocument)] and optionally refine it with
/// [#withTitle(String)], which replaces the label in navigation and search without altering
/// `<title>`. Page metadata such as the description belongs to the [PageDocument].
public record SiteRoute(String path, PageDocument document, @Nullable String title) {

  public SiteRoute {
    path = normalizePath(path);
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

  /// Normalizes a route path: trims it, adds a leading `/`, and removes one trailing `/` except on
  /// the root. Throws IllegalArgumentException if `rawPath` is blank.
  public static String normalizePath(String rawPath) {
    return PageAddress.of(rawPath).routePath();
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
    return PageAddress.of(routePath).documentUri();
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
}
