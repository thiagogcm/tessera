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
    int currentIdx = -1;
    for (int i = 0; i < routes.size(); i++) {
      if (routes.get(i).path().equals(normalizedCurrent)) {
        currentIdx = i;
        break;
      }
    }
    if (currentIdx < 0) {
      return DocPager.of(null, null);
    }
    DocPager.PageRef prev = null;
    if (currentIdx > 0) {
      SiteRoute p = routes.get(currentIdx - 1);
      String title = p.displayTitle();
      prev = new DocPager.PageRef(p.path(), title);
    }
    DocPager.PageRef next = null;
    if (currentIdx < routes.size() - 1) {
      SiteRoute n = routes.get(currentIdx + 1);
      String title = n.displayTitle();
      next = new DocPager.PageRef(n.path(), title);
    }
    return DocPager.of(prev, next);
  }
}
