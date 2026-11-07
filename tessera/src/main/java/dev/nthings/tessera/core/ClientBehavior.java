package dev.nthings.tessera.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/// ES module contributed to a document as browser behavior.
///
/// A behavior is identified by [#id] and emitted once per compiled page or site. When the page
/// publishes external assets, the module becomes a content-hashed file; otherwise it is inlined as
/// a module script.
public record ClientBehavior(String id, String module) {

  /// Loads the module bundled as `resource`, resolved relative to `anchor`'s package.
  public static ClientBehavior fromResource(String id, Class<?> anchor, String resource) {
    var input = anchor.getResourceAsStream(resource);
    if (input == null) {
      throw new IllegalArgumentException("Bundled client module not found: " + resource);
    }
    try (input) {
      return new ClientBehavior(id, new String(input.readAllBytes(), StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
