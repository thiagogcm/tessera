package dev.nthings.tessera.render;

import java.io.Serializable;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Authoring location retained through expansion, linking, and site validation.
/// Component paths name enclosing components; view entries include their instance scope.
/// `route` is absent for standalone page compilation.
public record CompilationLocation(
    String pageTitle,
    @Nullable String route,
    List<String> componentPath,
    String tagName,
    @Nullable String domId)
    implements Serializable {

  public CompilationLocation {
    componentPath = List.copyOf(componentPath);
  }

  public CompilationLocation atRoute(String path) {
    return new CompilationLocation(pageTitle, path, componentPath, tagName, domId);
  }
}
