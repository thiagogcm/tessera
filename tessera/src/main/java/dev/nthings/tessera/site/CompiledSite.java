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

/// Compiled routes and every other published file, keyed by URL path: assets, the shared
/// stylesheet, `sitemap.xml` and `search-index.json`.
public record CompiledSite(
    Map<String, CompiledPage> pagesByRoute,
    @Nullable String sharedCssPath,
    String sharedCssContent,
    SequencedMap<String, SiteAsset> files) {

  public CompiledSite {
    pagesByRoute = Collections.unmodifiableMap(new LinkedHashMap<>(pagesByRoute));
    files = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(files));
  }

  public @Nullable String sitemapXml() {
    return textFile(SITEMAP_PATH);
  }

  public @Nullable String searchIndexJson() {
    return textFile(SEARCH_INDEX_PATH);
  }

  private @Nullable String textFile(String path) {
    var file = files.get(path);
    return file == null ? null : new String(file.contentView(), StandardCharsets.UTF_8);
  }
}
