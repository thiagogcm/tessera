package dev.nthings.tessera.render;

import dev.nthings.tessera.core.SourceLocation;
import java.io.Serializable;
import java.util.List;
import java.util.StringJoiner;
import org.jspecify.annotations.Nullable;

/// Where an element sits among its components, independent of the placing page.
public record ViewLocation(
    List<String> componentPath,
    String tagName,
    @Nullable String domId,
    @Nullable SourceLocation source)
    implements Serializable {
  public ViewLocation {
    componentPath = List.copyOf(componentPath);
  }

  /// Compiler-facing: returns this location with its DOM id set to `id`.
  public ViewLocation withDomId(String id) {
    return new ViewLocation(componentPath, tagName, id, source);
  }

  /// `Outer > Inner > <tag id="x"> (File.java:42)`, omitting the parts that are absent.
  public String describe() {
    var path = new StringJoiner(" > ");
    componentPath.forEach(path::add);
    if (!tagName.isEmpty()) {
      path.add("<" + tagName + (domId == null ? "" : " id=\"" + domId + "\"") + ">");
    }
    return path + (source == null ? "" : " (" + source + ")");
  }
}
