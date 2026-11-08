package dev.nthings.tessera.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/// ES module emitted once per compiled page, identified by [#id]. A later behavior with the same id
/// is ignored silently. With `render.OutputMode.externalAsset` the module is written as a
/// content-hashed file, and each page's script tag references it; otherwise it is inlined as a
/// `<script type="module">`.
public record ClientBehavior(String id, String module) {

  /// Loads the module source bundled as `resource`, resolved relative to the package of `anchor`.
  /// Throws IllegalArgumentException if the resource is missing.
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
