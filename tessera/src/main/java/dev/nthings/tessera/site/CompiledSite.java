package dev.nthings.tessera.site;

import static dev.nthings.tessera.site.SiteArtifacts.SEARCH_INDEX_PATH;
import static dev.nthings.tessera.site.SiteArtifacts.SITEMAP_PATH;

import dev.nthings.tessera.render.CompiledPage;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import org.jspecify.annotations.Nullable;

/// The output of [SiteCompiler]. `pagesByRoute` maps each route path to its page. `files` maps each
/// published URL path to its file: static assets, the shared stylesheet, `sitemap.xml` and
/// `search-index.json`. `sharedCssPath` is null under inline output, where each page embeds the
/// stylesheet itself; `sharedCssContent` is always present.
public record CompiledSite(
    Map<String, CompiledPage> pagesByRoute,
    @Nullable String sharedCssPath,
    String sharedCssContent,
    SequencedMap<String, SiteAsset> files) {

  public CompiledSite {
    pagesByRoute = Collections.unmodifiableMap(new LinkedHashMap<>(pagesByRoute));
    files = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(files));
  }

  /// The text of `sitemap.xml`, or null when the site has no base URL.
  public @Nullable String sitemapXml() {
    return textFile(SITEMAP_PATH);
  }

  /// The text of `search-index.json`. It is always present.
  public @Nullable String searchIndexJson() {
    return textFile(SEARCH_INDEX_PATH);
  }

  private @Nullable String textFile(String path) {
    var file = files.get(path);
    return file == null ? null : new String(file.contentView(), StandardCharsets.UTF_8);
  }
}
