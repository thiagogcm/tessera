package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.SourceLocation;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Where an element sits among its components, independent of the placing page. Compilation
/// turns it into a [Location] once the page is known.
record NodeLocation(
    List<String> componentPath,
    String tagName,
    @Nullable String domId,
    @Nullable SourceLocation source) {
  NodeLocation {
    componentPath = List.copyOf(componentPath);
  }

  /// Returns this location with its DOM id set to `id`.
  NodeLocation withDomId(String id) {
    return new NodeLocation(componentPath, tagName, id, source);
  }
}
