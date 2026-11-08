package dev.nthings.tessera.render;

import java.io.Serializable;
import org.jspecify.annotations.Nullable;

/// Where a diagnostic occurred: the page title, the route (null for standalone page compilation),
/// and the element's [ViewLocation]. The authoring call site, when tracked, is
/// [ViewLocation#source()].
public record CompilationLocation(String pageTitle, @Nullable String route, ViewLocation view)
    implements Serializable {

  /// Compiler-facing: returns this location with its route set to `path`.
  public CompilationLocation atRoute(String path) {
    return new CompilationLocation(pageTitle, path, view);
  }

  /// `Route "/x", page "Title" > Component > <tag id="y"> (File.java:42)`.
  public String describe() {
    var where = view.describe();
    return (route != null ? "Route \"" + route + "\", " : "")
        + "page \""
        + pageTitle
        + "\""
        + (where.isEmpty() ? "" : " > " + where);
  }
}
