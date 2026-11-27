package dev.nthings.tessera.document;

import com.google.errorprone.annotations.CheckReturnValue;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/// ES module emitted once per compiled page, identified by [#id]. A later script with the same id
/// is ignored silently. With `compiler.OutputMode.external` the module is written as a
/// content-hashed file, and each page's script tag references it; otherwise it is inlined as a
/// `<script type="module">`.
///
/// @param imports the vendored [ScriptModule]s the module imports by name. Each is published as a
///   content-hashed file, and the page's import map resolves its name.
public record PageScript(String id, String module, List<ScriptModule> imports) {

  public PageScript {
    if (id.isBlank()) {
      throw new IllegalArgumentException("Page script id cannot be blank");
    }
    imports = List.copyOf(imports);
  }

  /// The script `id` running `module`, with no vendored imports.
  public static PageScript of(String id, String module) {
    return new PageScript(id, module, List.of());
  }

  /// Loads the module source bundled as `resource`, resolved relative to the package of `anchor`.
  /// Throws IllegalArgumentException if the resource is missing.
  public static PageScript fromResource(String id, Class<?> anchor, String resource) {
    return of(id, readResource(anchor, resource));
  }

  /// Returns a copy that also imports `modules`, after any earlier imports.
  @CheckReturnValue
  public PageScript withImports(ScriptModule... modules) {
    var all = new ArrayList<>(imports);
    all.addAll(List.of(modules));
    return new PageScript(id, module, all);
  }

  static String readResource(Class<?> anchor, String resource) {
    var input = anchor.getResourceAsStream(resource);
    if (input == null) {
      throw new IllegalArgumentException("Bundled client module not found: " + resource);
    }
    try (input) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
