package dev.nthings.tessera.document;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/// ES module emitted once per compiled page, identified by [#id]. A later script with the same id
/// is ignored silently. With `compiler.OutputMode.external` the module is written as a
/// content-hashed file, and each page's script tag references it; otherwise it is inlined as a
/// `<script type="module">`.
public record PageScript(String id, String module) {

  /// Loads the module source bundled as `resource`, resolved relative to the package of `anchor`.
  /// Throws IllegalArgumentException if the resource is missing.
  public static PageScript fromResource(String id, Class<?> anchor, String resource) {
    var input = anchor.getResourceAsStream(resource);
    if (input == null) {
      throw new IllegalArgumentException("Bundled client module not found: " + resource);
    }
    try (input) {
      return new PageScript(id, new String(input.readAllBytes(), StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
