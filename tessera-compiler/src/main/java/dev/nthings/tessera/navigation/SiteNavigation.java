package dev.nthings.tessera.navigation;

import dev.nthings.tessera.compiler.SiteDefinition;
import dev.nthings.tessera.compiler.SiteRoute;
import dev.nthings.tessera.components.DocPager;
import dev.nthings.tessera.components.Link;
import dev.nthings.tessera.components.SearchPalette;
import java.util.List;

/// Navigation components derived from a [SiteDefinition]'s routes.
public final class SiteNavigation {

  private static final String DEFAULT_SEARCH_GROUP = "Pages & Guides";

  private SiteNavigation() {}

  /// `SearchPalette` listing every route of the given [SiteDefinition]; see [#searchPalette(List)].
  public static SearchPalette searchPalette(SiteDefinition site) {
    return searchPalette(site.routes(), DEFAULT_SEARCH_GROUP);
  }

  /// `SearchPalette` listing every route of `site` under `group`.
  public static SearchPalette searchPalette(SiteDefinition site, String group) {
    return searchPalette(site.routes(), group);
  }

  /// `SearchPalette` over the given routes, under a "Pages & Guides" group. Each route links to its
  /// published URL ([SiteRoute#documentUri()]), with its description and path shown beside it. Set
  /// an id or other options on the returned palette.
  public static SearchPalette searchPalette(List<SiteRoute> routes) {
    return searchPalette(routes, DEFAULT_SEARCH_GROUP);
  }

  /// `SearchPalette` over `routes` under `group`. Each route links to its published URL
  /// ([SiteRoute#documentUri()]).
  public static SearchPalette searchPalette(List<SiteRoute> routes, String group) {
    var palette = SearchPalette.of();
    for (var route : routes) {
      var href = route.documentUri().toASCIIString();
      var item = SearchPalette.Item.of(route.displayTitle(), href).badge(route.path());
      var description = route.document().description();
      palette.item(group, description != null ? item.description(description) : item);
    }
    return palette;
  }

  /// Previous/next pager for `currentPath` in route declaration order. The first route has no
  /// previous link and the last has no next link. The path is normalized like a route path, so a
  /// trailing `/` is accepted. Throws IllegalArgumentException if no route is registered at it.
  public static DocPager docPager(SiteDefinition site, String currentPath) {
    return docPager(site.routes(), currentPath);
  }

  /// Previous/next pager for `currentPath` in `routes` declaration order. Throws
  /// IllegalArgumentException if no route matches `currentPath`.
  public static DocPager docPager(List<SiteRoute> routes, String currentPath) {
    var target = SiteRoute.normalizePath(currentPath);
    var index = -1;
    for (var i = 0; i < routes.size(); i++) {
      if (routes.get(i).path().equals(target)) {
        index = i;
        break;
      }
    }
    if (index < 0) {
      throw new IllegalArgumentException("No route registered at " + currentPath);
    }
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
    return new Link(route.documentUri().toASCIIString(), route.displayTitle());
  }
}
