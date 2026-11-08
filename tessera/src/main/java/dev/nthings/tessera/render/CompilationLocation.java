package dev.nthings.tessera.render;

import java.io.Serializable;
import org.jspecify.annotations.Nullable;

/// A view location on a page; `route` is absent for standalone page compilation.
///
/// Source is the authoring call site, present only while [SourceTracking] is active.
public record CompilationLocation(String pageTitle, @Nullable String route, ViewLocation view)
    implements Serializable {

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
