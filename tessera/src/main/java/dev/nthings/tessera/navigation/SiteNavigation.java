package dev.nthings.tessera.navigation;

import dev.nthings.tessera.compiler.SiteDefinition;
import dev.nthings.tessera.compiler.SiteRoute;
import dev.nthings.tessera.components.DocPager;
import dev.nthings.tessera.components.Link;
import dev.nthings.tessera.components.SearchPalette;
import java.util.List;

/// Navigation components derived from a [SiteDefinition]'s routes.
public final class SiteNavigation {

  private SiteNavigation() {}

  /// `SearchPalette` listing every route of the given [SiteDefinition]; see
  /// [#searchPalette(List)].
  public static SearchPalette searchPalette(SiteDefinition site) {
    return searchPalette(site.routes());
  }

  /// `SearchPalette` over the given routes, under a "Pages & Guides" group. Each route is a link
  /// to its path, with its description and path shown beside it. Set an id or other options on
  /// the returned palette.
  public static SearchPalette searchPalette(List<SiteRoute> routes) {
    var palette = SearchPalette.of();
    for (var route : routes) {
      var item = SearchPalette.Item.of(route.displayTitle(), route.path()).badge(route.path());
      var description = route.description();
      palette.item("Pages & Guides", description != null ? item.description(description) : item);
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
    var pager = DocPager.of();
    if (index > 0) {
      pager.previous(link(routes.get(index - 1)));
    }
    if (index < routes.size() - 1) {
      pager.next(link(routes.get(index + 1)));
    }
    return pager;
  }

  private static Link link(SiteRoute route) {
    return new Link(route.path(), route.displayTitle());
  }
}
