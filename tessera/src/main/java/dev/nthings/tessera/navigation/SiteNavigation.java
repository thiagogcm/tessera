package dev.nthings.tessera.navigation;

import dev.nthings.tessera.compiler.SiteDefinition;
import dev.nthings.tessera.compiler.SiteRoute;
import dev.nthings.tessera.components.DocPager;
import dev.nthings.tessera.components.SearchPalette;
import java.util.List;

/// Navigation components derived from a [SiteDefinition]'s routes.
public final class SiteNavigation {

  private SiteNavigation() {}

  /// `SearchPalette` listing every route of the given [SiteDefinition], with the fixed id
  /// `search-palette`. Two palettes with that id on one page collide, so use
  /// [#searchPalette(String, List)] for a second one.
  public static SearchPalette searchPalette(SiteDefinition site) {
    return searchPalette("search-palette", site.routes());
  }

  /// `SearchPalette` over the given routes, under a "Pages & Guides" group. Each route is a link
  /// to its path, with its description and path shown beside it.
  public static SearchPalette searchPalette(String id, List<SiteRoute> routes) {
    var palette = SearchPalette.create(id);
    for (var route : routes) {
      var title = route.displayTitle();
      palette.item("Pages & Guides", title, route.path(), route.description(), route.path());
    }
    return palette;
  }

  /// Previous/next pager for `currentPath` in route declaration order. The first route has no
  /// previous link and the last has no next link. The path is normalized like a route path, so a
  /// trailing `/` is accepted. Throws IllegalArgumentException if no route is registered at it.
  public static DocPager docPager(SiteDefinition site, String currentPath) {
    var current =
        site.route(currentPath)
            .orElseThrow(
                () -> new IllegalArgumentException("No route registered at " + currentPath));
    var routes = site.routes();
    var index = routes.indexOf(current);
    return DocPager.of(
        index > 0 ? pageRef(routes.get(index - 1)) : null,
        index < routes.size() - 1 ? pageRef(routes.get(index + 1)) : null);
  }

  private static DocPager.PageRef pageRef(SiteRoute route) {
    return new DocPager.PageRef(route.path(), route.displayTitle());
  }
}
