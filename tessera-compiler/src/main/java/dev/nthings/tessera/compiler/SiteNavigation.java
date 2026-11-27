package dev.nthings.tessera.compiler;

import dev.nthings.tessera.components.DocPager;
import dev.nthings.tessera.components.Link;
import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.document.PageAddress;
import java.util.List;

/// Navigation components derived from a site's routes, such as [SiteDefinition#routes()].
public final class SiteNavigation {

  private SiteNavigation() {}

  /// `SearchPalette` over `routes`, such as [SiteDefinition#routes()], under `group`. Each route
  /// links to its published URL ([SiteRoute#documentUri()]), with its description and path shown
  /// beside it. Set an id or other options on the returned palette.
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

  /// Previous/next pager around the page at `current` in `routes` declaration order. The first
  /// route has no previous link and the last has no next link. Inside a document callback, pass
  /// `context.address().orElseThrow()`. Throws IllegalArgumentException if no route is published at
  /// `current`.
  public static DocPager docPager(List<SiteRoute> routes, PageAddress current) {
    var index = -1;
    for (var i = 0; i < routes.size(); i++) {
      if (routes.get(i).path().equals(current.routePath())) {
        index = i;
        break;
      }
    }
    if (index < 0) {
      throw new IllegalArgumentException("No route registered at " + current.routePath());
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
