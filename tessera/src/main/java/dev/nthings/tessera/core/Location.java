package dev.nthings.tessera.core;

import java.io.Serializable;
import java.util.List;
import java.util.StringJoiner;
import org.jspecify.annotations.Nullable;

/// Where an element, component or site diagnostic occurred. Page and route context are optional.
public record Location(
    @Nullable String route,
    @Nullable String page,
    List<String> components,
    String tag,
    @Nullable String domId,
    @Nullable SourceLocation source)
    implements Serializable {
  /// The top level of an expansion.
  public static final Location ROOT = new Location(null, null, List.of(), "", null, null);

  public Location {
    components = List.copyOf(components);
  }

  /// An element or component location before a page or route is attached.
  public Location(
      List<String> components,
      String tag,
      @Nullable String domId,
      @Nullable SourceLocation source) {
    this(null, null, components, tag, domId, source);
  }

  /// A site-level problem without an element or component.
  public static Location site(@Nullable String route, @Nullable String page) {
    return new Location(route, page, List.of(), "", null, null);
  }

  /// This location in a page.
  public Location atPage(String title) {
    return new Location(route, title, components, tag, domId, source);
  }

  /// This location at a site route.
  public Location atRoute(String path) {
    return new Location(path, page, components, tag, domId, source);
  }

  /// This location with its bound DOM id.
  public Location withDomId(String id) {
    return new Location(route, page, components, tag, id, source);
  }

  /// The route, page, component path, element and source, omitting absent context.
  public String describe() {
    var head = new StringJoiner(", ");
    if (route != null) {
      head.add("Route \"" + route + "\"");
    }
    if (page != null) {
      head.add("page \"" + page + "\"");
    }
    var parts = new StringJoiner(" > ");
    if (head.length() > 0) {
      parts.add(head.toString());
    }
    components.forEach(parts::add);
    if (!tag.isEmpty()) {
      parts.add("<" + tag + (domId == null ? "" : " id=\"" + domId + "\"") + ">");
    }
    return parts + (source == null ? "" : " (" + source + ")");
  }
}
