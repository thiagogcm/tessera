/// Multi-page static sites. Describe routes and assets with [SiteDefinition#builder()], then
/// compile with [SiteCompiler#compile(SiteDefinition)], which prepares pages concurrently,
/// validates links to routes and assets, fragment targets and transition keys across pages, and
/// adds the shared stylesheet (under external output), `search-index.json`, and `sitemap.xml`
/// when a base URL is set. Write the [CompiledSite] with [SiteWriter], or serve it locally with
/// [SitePreviewServer].
@NullMarked
@SourceBoundary
package dev.nthings.tessera.site;

import dev.nthings.tessera.core.SourceBoundary;
import org.jspecify.annotations.NullMarked;
