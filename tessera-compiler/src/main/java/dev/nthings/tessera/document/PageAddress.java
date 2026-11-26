package dev.nthings.tessera.document;

import com.google.errorprone.annotations.CheckReturnValue;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/// Where a page is published: its normalized route path, its browser URL, and, when the site has a
/// base URL, its absolute URL. Site compilation gives every route's document callbacks an address
/// through [DocumentContext#address()], so a shared layout can build absolute links or mark the
/// current navigation entry without receiving the path as a parameter.
///
/// The route path is normalized like a site route: it is trimmed, gains a leading `/`, and loses
/// one trailing `/` except on the root. Directory routes publish at a URL ending in `/`; routes
/// ending in `.html` keep their path.
///
/// | Route path | [#documentUri()] | [#absoluteUrl()] under `https://example.com` |
/// | --- | --- | --- |
/// | `/` | `/` | `https://example.com/` |
/// | `/guide` | `/guide/` | `https://example.com/guide/` |
/// | `/about.html` | `/about.html` | `https://example.com/about.html` |
public final class PageAddress {

  private final String routePath;
  private final URI documentUri;
  private final @Nullable URI absoluteUrl;

  private PageAddress(String routePath, @Nullable URI baseUrl) {
    this.routePath = normalize(routePath);
    this.documentUri = documentUriOf(this.routePath);
    this.absoluteUrl =
        baseUrl == null
            ? null
            : URI.create(stripTrailingSlash(baseUrl.toASCIIString()) + documentUri.toASCIIString());
  }

  /// The address of the route at `routePath`, without an absolute URL. Throws
  /// IllegalArgumentException if `routePath` is blank or not a valid URL path.
  public static PageAddress of(String routePath) {
    return new PageAddress(routePath, null);
  }

  /// Returns this address published under `baseUrl`, such as `https://example.com`. A trailing `/`
  /// on the base URL is ignored. Throws IllegalArgumentException unless `baseUrl` is an absolute
  /// `http` or `https` origin: a host and no user info, path, query, or fragment. Route paths and
  /// authored links are not rewritten for subdirectory hosting, so a base path would publish URLs
  /// that the pages do not link to.
  @CheckReturnValue
  public PageAddress withBaseUrl(URI baseUrl) {
    var scheme = baseUrl.getScheme();
    if (scheme == null
        || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
        || baseUrl.getHost() == null
        || baseUrl.getRawUserInfo() != null
        || !(baseUrl.getRawPath() == null
            || baseUrl.getRawPath().isEmpty()
            || baseUrl.getRawPath().equals("/"))
        || baseUrl.getRawQuery() != null
        || baseUrl.getRawFragment() != null) {
      throw new IllegalArgumentException(
          "Base URL must be an absolute http(s) URL such as https://example.com: " + baseUrl);
    }
    return new PageAddress(routePath, baseUrl);
  }

  /// The normalized route path, such as `/guide`.
  public String routePath() {
    return routePath;
  }

  /// The browser URL of the page relative to the site root, such as `/guide/`.
  public URI documentUri() {
    return documentUri;
  }

  /// The absolute URL of the page, such as `https://example.com/guide/`, or empty when the site has
  /// no base URL.
  public Optional<URI> absoluteUrl() {
    return Optional.ofNullable(absoluteUrl);
  }

  /// Whether `href`, written as it would appear on this page, links to this page. The link is
  /// resolved against [#documentUri()] the way browsers and site link validation resolve it: the
  /// query and fragment are ignored, `index.html` names its directory, and `/guide` matches the
  /// `/guide/` page. An absolute link matches only when it has the origin of [#absoluteUrl()]. A
  /// reference with only a query or fragment, such as `#install`, stays on this page. A malformed
  /// `href` does not match.
  public boolean isCurrent(String href) {
    URI reference;
    try {
      reference = new URI(href);
    } catch (URISyntaxException e) {
      return false;
    }
    String path;
    if (reference.isAbsolute() || reference.getRawAuthority() != null) {
      if (absoluteUrl == null) {
        return false;
      }
      var resolved = absoluteUrl.resolve(reference);
      if (!sameOrigin(resolved, absoluteUrl)) {
        return false;
      }
      path = resolved.normalize().getPath();
    } else {
      var rawPath = reference.getRawPath();
      if (rawPath == null || rawPath.isEmpty()) {
        return true;
      }
      path = documentUri.resolve(reference).normalize().getPath();
    }
    if (path == null || path.isEmpty()) {
      path = "/";
    }
    path = withinRoot(path);
    if (path.endsWith("/index.html")) {
      path = path.substring(0, path.length() - "index.html".length());
    }
    var current = Objects.requireNonNull(documentUri.getPath());
    return path.equals(current) || (path + "/").equals(current);
  }

  private static boolean sameOrigin(URI a, URI b) {
    return a.getScheme() != null
        && a.getScheme().equalsIgnoreCase(b.getScheme())
        && a.getHost() != null
        && a.getHost().equalsIgnoreCase(b.getHost())
        && effectivePort(a) == effectivePort(b);
  }

  private static int effectivePort(URI uri) {
    if (uri.getPort() >= 0) {
      return uri.getPort();
    }
    return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
  }

  /// `path` with the `..` segments that climb above the root dropped, as browsers resolve them.
  private static String withinRoot(String path) {
    var resolved = path;
    while (resolved.startsWith("/../") || resolved.equals("/..")) {
      resolved = resolved.substring(3);
    }
    return resolved.isEmpty() ? "/" : resolved;
  }

  private static String normalize(String rawPath) {
    if (rawPath.isBlank()) {
      throw new IllegalArgumentException("Route path cannot be blank");
    }
    var path = rawPath.trim();
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    if (path.length() > 1 && path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    return path;
  }

  private static URI documentUriOf(String routePath) {
    var documentPath =
        routePath.endsWith(".html") || routePath.endsWith("/") ? routePath : routePath + "/";
    try {
      return new URI(null, null, documentPath, null);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid route path: " + routePath, e);
    }
  }

  private static String stripTrailingSlash(String text) {
    return text.endsWith("/") ? text.substring(0, text.length() - 1) : text;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof PageAddress address
        && routePath.equals(address.routePath)
        && Objects.equals(absoluteUrl, address.absoluteUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(routePath, absoluteUrl);
  }

  @Override
  public String toString() {
    return absoluteUrl != null ? absoluteUrl.toString() : documentUri.toString();
  }
}
