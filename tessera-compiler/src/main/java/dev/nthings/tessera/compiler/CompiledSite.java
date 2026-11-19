package dev.nthings.tessera.compiler;

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
///   under [OutputMode#external(String)] it is also one of the `files`.
/// - `files`: each published URL path mapped to its file: registered assets, the shared stylesheet
///   and page scripts (external output), published browser bundles, `search-index.json`, and
///   `sitemap.xml` when the site has a base URL.
/// - `pageHeaders`: deployment response headers keyed by route. Apply them to each HTML response.
///
/// [SiteWriter] and [SitePreviewServer] publish pages and files together.
public record CompiledSite(
    SequencedMap<String, String> pages,
    String css,
    SequencedMap<String, Asset> files,
    Map<String, Map<String, String>> pageHeaders) {

  public CompiledSite {
    pages = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(pages));
    files = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(files));
    var headers = new LinkedHashMap<String, Map<String, String>>();
    pageHeaders.forEach((route, values) -> headers.put(route, Map.copyOf(values)));
    pageHeaders = Collections.unmodifiableMap(headers);
  }

  /// The text of `sitemap.xml`, or null when the site has no base URL.
  public @Nullable String sitemapXml() {
    var file = files.get(SITEMAP_PATH);
    return file == null ? null : file.textContent();
  }

  /// The text of `search-index.json`, which every compiled site has.
  public String searchIndexJson() {
    return Objects.requireNonNull(files.get(SEARCH_INDEX_PATH), SEARCH_INDEX_PATH).textContent();
  }
}
