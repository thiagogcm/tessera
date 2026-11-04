package dev.nthings.tessera.site;

import dev.nthings.tessera.components.DocPager;
import dev.nthings.tessera.components.SearchPalette;
import java.util.List;

/// Navigation components derived from a [SiteDefinition]'s routes.
public final class SiteNavigation {

  private SiteNavigation() {}

  /// Automatically initializes a `SearchPalette` indexed from all routes in the given
  /// [SiteDefinition].
  public static SearchPalette searchPalette(SiteDefinition site) {
    return searchPalette("search-palette", site.routes());
  }

  /// Initializes a `SearchPalette` indexed with the given route list under a "Pages & Guides"
  /// group.
  public static SearchPalette searchPalette(String id, List<SiteRoute> routes) {
    var palette = SearchPalette.create(id);
    for (var route : routes) {
      String title = route.displayTitle();
      palette.item("Pages & Guides", title, route.path(), route.description(), route.path());
    }
    return palette;
  }

  /// Previous/next pager for `currentPath` in route declaration order.
  public static DocPager docPager(SiteDefinition site, String currentPath) {
    String normalizedCurrent = SiteRoute.normalizeRoutePath(currentPath);
    List<SiteRoute> routes = site.routes();
    int index = routes.stream().map(SiteRoute::path).toList().indexOf(normalizedCurrent);
    if (index < 0) {
      throw new IllegalArgumentException("No route registered at " + normalizedCurrent);
    }
    return DocPager.of(
        index > 0 ? pageRef(routes.get(index - 1)) : null,
        index < routes.size() - 1 ? pageRef(routes.get(index + 1)) : null);
  }

  private static DocPager.PageRef pageRef(SiteRoute route) {
    return new DocPager.PageRef(route.path(), route.displayTitle());
  }
}
