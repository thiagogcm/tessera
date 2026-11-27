package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.compiler.SiteArtifacts.HEADERS_PATH;
import static dev.nthings.tessera.compiler.SiteArtifacts.SEARCH_INDEX_PATH;
import static dev.nthings.tessera.compiler.SiteArtifacts.SITEMAP_PATH;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import org.jspecify.annotations.Nullable;

/// The output of [Compiler#compile(SiteDefinition)].
///
/// - `pages`: each route path mapped to its HTML document, in route declaration order.
/// - `css`: the stylesheet every page shares. Under [OutputMode#inline()] each page embeds it;
///   under [OutputMode#external(String)] it is also one of the `assets`.
/// - `assets`: each published URL path mapped to its file: registered assets, the shared stylesheet
///   and page scripts (external output), published browser bundles, `search-index.json`, and
///   `sitemap.xml` when the site has a base URL.
/// - `notFoundPage`: the HTML of the [SiteDefinition#withNotFoundPage(PageDocument)] page,
///   published as `/404.html`, or null.
/// - `headers`: deployment response headers keyed by route (and `/404.html`). Apply them to each
///   HTML response.
/// - `siteHeaders`: the [SiteDefinition#headers()] every response gets.
///
/// The `/_headers` asset holds every rule in the format Cloudflare Pages and Netlify read: the site
/// headers for `/*`, `Cache-Control: public, max-age=31536000, immutable` for the content-hashed
/// files the compiler generates, and each route's own headers.
///
/// [SiteWriter] and [SitePreviewServer] publish pages and assets together.
public record CompiledSite(
    SequencedMap<String, String> pages,
    @Nullable String notFoundPage,
    String css,
    SequencedMap<String, Asset> assets,
    Map<String, Map<String, String>> headers,
    SequencedMap<String, String> siteHeaders) {

  public CompiledSite {
    pages = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(pages));
    siteHeaders = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(siteHeaders));
    assets = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(assets));
    var byRoute = new LinkedHashMap<String, Map<String, String>>();
    headers.forEach((route, values) -> byRoute.put(route, Map.copyOf(values)));
    headers = Collections.unmodifiableMap(byRoute);
  }

  /// The text of `sitemap.xml`, or null when the site has no base URL.
  public @Nullable String sitemapXml() {
    var file = assets.get(SITEMAP_PATH);
    return file == null ? null : file.textContent();
  }

  /// The text of `_headers`, which every compiled site has.
  public String headersFile() {
    return Objects.requireNonNull(assets.get(HEADERS_PATH), HEADERS_PATH).textContent();
  }

  /// The text of `search-index.json`, which every compiled site has.
  public String searchIndexJson() {
    return Objects.requireNonNull(assets.get(SEARCH_INDEX_PATH), SEARCH_INDEX_PATH).textContent();
  }
}
