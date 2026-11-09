package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.SourceLocation;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Where an element sits among its components, independent of the placing page. Compilation
/// turns it into a [Location] once the page is known.
record ViewLocation(
    List<String> componentPath,
    String tagName,
    @Nullable String domId,
    @Nullable SourceLocation source) {
  ViewLocation {
    componentPath = List.copyOf(componentPath);
  }

  /// Returns this location with its DOM id set to `id`.
  ViewLocation withDomId(String id) {
    return new ViewLocation(componentPath, tagName, id, source);
  }
}
