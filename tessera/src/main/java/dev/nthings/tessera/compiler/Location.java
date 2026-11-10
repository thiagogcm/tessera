package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.SourceLocation;
import java.io.Serializable;
import java.util.List;
import java.util.StringJoiner;
import org.jspecify.annotations.Nullable;

/// Where a [Diagnostic] occurred.
///
/// - `route`: the site route path, or null for standalone page compilation and for site-level
///   file diagnostics.
/// - `page`: the page title, or null for site-level file diagnostics that belong to no page.
/// - `components`: the enclosing components, outermost first; empty outside any component.
/// - `tag`: the element's tag name, or empty when the diagnostic concerns a component or the site.
/// - `domId`: the element's DOM id, when it has one.
/// - `source`: the Java line that built the element, recorded only while [SourceTracking] is on.
public record Location(
    @Nullable String route,
    @Nullable String page,
    List<String> components,
    String tag,
    @Nullable String domId,
    @Nullable SourceLocation source)
    implements Serializable {

  public Location {
    components = List.copyOf(components);
  }

  /// An element or component location on a page.
  Location(
      @Nullable String page,
      List<String> components,
      String tag,
      @Nullable String domId,
      @Nullable SourceLocation source) {
    this(null, page, components, tag, domId, source);
  }

  /// Location of a site-level problem that belongs to no page, such as a file path collision.
  static Location site(@Nullable String route, @Nullable String page) {
    return new Location(route, page, List.of(), "", null, null);
  }

  /// Returns this location with its DOM id set to `id`.
  Location withDomId(String id) {
    return new Location(route, page, components, tag, id, source);
  }

  Location atRoute(String path) {
    return new Location(path, page, components, tag, domId, source);
  }

  /// `Route "/x", page "Title" > Component > <tag id="y"> (File.java:42)`, omitting the parts that
  /// are absent.
  public String describe() {
    var parts = new StringJoiner(" > ");
    var head = new StringJoiner(", ");
    if (route != null) {
      head.add("Route \"" + route + "\"");
    }
    if (page != null) {
      head.add("page \"" + page + "\"");
    }
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
