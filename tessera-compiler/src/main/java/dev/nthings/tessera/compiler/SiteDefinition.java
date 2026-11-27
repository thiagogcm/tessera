package dev.nthings.tessera.compiler;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.document.PageAddress;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/// Multi-page static site specification. Create one with [#of(SiteRoute...)] and adjust it with the
/// `with` methods. Throws IllegalArgumentException if it has no routes.
///
/// Compilation settings ([dev.nthings.tessera.css.Theme] and [OutputMode]) live on [Compiler].
/// Defaults to [LinkSpeculation#PRERENDER].
///
/// @param routes the pages in declaration order, which sets the order of diagnostics, `sitemap.xml`
///   entries and `search-index.json` entries. Two routes with one path are reported as
///   `duplicate-route` when the site is compiled.
/// @param baseUrl the site origin without a trailing `/`, or `null`; it enables `sitemap.xml` and
///   the default canonical URL of each page
/// @param assets files published beside the pages. A file that shares its path with a route or
///   another file is reported as `file-path-collision` when the site is compiled.
/// @param fonts the self-hosted font faces, whose files are published beside the assets
/// @param notFoundPage the page published as `/404.html`, which static hosts serve for unknown
///   URLs, or `null` for none
/// @param headers the response headers every page and file gets, written to `/_headers` and applied
///   by [SitePreviewServer]
public record SiteDefinition(
    List<SiteRoute> routes,
    @Nullable URI baseUrl,
    LinkSpeculation linkSpeculation,
    List<Asset> assets,
    List<FontFace> fonts,
    @Nullable PageDocument notFoundPage,
    SequencedMap<String, String> headers) {

  /// The headers a site sends by default: `X-Content-Type-Options: nosniff` and `Referrer-Policy:
  /// strict-origin-when-cross-origin`.
  public static final SequencedMap<String, String> DEFAULT_HEADERS = defaultHeaders();

  private static final Pattern HEADER_NAME = Pattern.compile("[!#$%&'*+.^_`|~0-9A-Za-z-]+");

  /// Throws IllegalArgumentException if a header name is not an HTTP token or a value holds a line
  /// break.
  public SiteDefinition {
    if (routes.isEmpty()) {
      throw new IllegalArgumentException("SiteDefinition must contain at least one route");
    }
    routes = List.copyOf(routes);
    assets = List.copyOf(assets);
    fonts = List.copyOf(fonts);
    if (baseUrl != null) {
      baseUrl = normalizeBaseUrl(baseUrl);
    }
    var checked = new LinkedHashMap<String, String>();
    headers.forEach((name, value) -> {
      if (!HEADER_NAME.matcher(name).matches()) {
        throw new IllegalArgumentException("Invalid response header name: " + name);
      }
      if (value.contains("\n") || value.contains("\r")) {
        throw new IllegalArgumentException("Response header " + name + " holds a line break");
      }
      checked.put(name, value.strip());
    });
    headers = Collections.unmodifiableSequencedMap(checked);
  }

  private static SequencedMap<String, String> defaultHeaders() {
    var headers = new LinkedHashMap<String, String>();
    headers.put("X-Content-Type-Options", "nosniff");
    headers.put("Referrer-Policy", "strict-origin-when-cross-origin");
    return Collections.unmodifiableSequencedMap(headers);
  }

  /// `uri` without its trailing `/`, validated as [PageAddress#withBaseUrl(URI)] validates it.
  private static URI normalizeBaseUrl(URI uri) {
    var root = PageAddress.of("/").withBaseUrl(uri).absoluteUrl().orElseThrow().toASCIIString();
    return URI.create(root.substring(0, root.length() - 1));
  }

  /// The route published at `path`, normalized like a route path (so `guide/` finds `/guide`), or
  /// empty when no route has that path. Throws IllegalArgumentException if `path` is blank.
  public Optional<SiteRoute> route(String path) {
    var normalized = SiteRoute.normalizePath(path);
    return routes.stream().filter(route -> route.path().equals(normalized)).findFirst();
  }

  /// Returns a site with `routes` and default settings: no base URL, [LinkSpeculation#PRERENDER],
  /// no assets, no fonts, no 404 page and the [#DEFAULT_HEADERS].
  public static SiteDefinition of(SiteRoute... routes) {
    return of(List.of(routes));
  }

  /// Returns a site with `routes`, in iteration order, and default settings.
  public static SiteDefinition of(Collection<SiteRoute> routes) {
    return new SiteDefinition(
        List.copyOf(routes),
        null,
        LinkSpeculation.PRERENDER,
        List.of(),
        List.of(),
        null,
        DEFAULT_HEADERS);
  }

  /// Returns a copy with `baseUrl`, which is prepended to each page's published URL in
  /// `sitemap.xml` and in each page's `<link rel="canonical">`, such as
  /// `URI.create("https://example.com")`. A trailing `/` is removed. Throws
  /// IllegalArgumentException unless the value is an absolute `http` or `https` URI with a host and
  /// no user info, path, query, or fragment.
  @CheckReturnValue
  public SiteDefinition withBaseUrl(URI baseUrl) {
    return new SiteDefinition(
        routes, baseUrl, linkSpeculation, assets, fonts, notFoundPage, headers);
  }

  /// Returns a copy with the same-site speculation rules added to each page's `<head>`.
  @CheckReturnValue
  public SiteDefinition withLinkSpeculation(LinkSpeculation linkSpeculation) {
    return new SiteDefinition(
        routes, baseUrl, linkSpeculation, assets, fonts, notFoundPage, headers);
  }

  /// Returns a copy that publishes `assets` beside the pages, replacing any earlier assets.
  @CheckReturnValue
  public SiteDefinition withAssets(List<Asset> assets) {
    return new SiteDefinition(
        routes, baseUrl, linkSpeculation, assets, fonts, notFoundPage, headers);
  }

  /// Returns a copy with the self-hosted `fonts`, replacing any earlier fonts. Each file is
  /// published like an asset, every page's stylesheet declares each face, and a preloaded face
  /// ([FontFace#withPreload()]) is preloaded by every page.
  @CheckReturnValue
  public SiteDefinition withFonts(List<FontFace> fonts) {
    return new SiteDefinition(
        routes, baseUrl, linkSpeculation, assets, fonts, notFoundPage, headers);
  }

  /// Returns a copy that publishes `page` as `/404.html`, the file Cloudflare Pages, Netlify and
  /// most static hosts serve, with status 404, for a URL that matches nothing. Build it with the
  /// site's own layout. The host serves it at any depth, so its links must be root-relative (`/`)
  /// or absolute; a relative link reports `not-found-relative-link`. It stays out of `sitemap.xml`
  /// and `search-index.json`.
  @CheckReturnValue
  public SiteDefinition withNotFoundPage(PageDocument page) {
    return new SiteDefinition(routes, baseUrl, linkSpeculation, assets, fonts, page, headers);
  }

  /// Returns a copy that sends `headers` with every page and file, replacing the earlier ones (by
  /// default [#DEFAULT_HEADERS]), such as a `Content-Security-Policy` or `Permissions-Policy`. Pass
  /// an empty map to send none.
  @CheckReturnValue
  public SiteDefinition withHeaders(SequencedMap<String, String> headers) {
    return new SiteDefinition(
        routes, baseUrl, linkSpeculation, assets, fonts, notFoundPage, headers);
  }
}
