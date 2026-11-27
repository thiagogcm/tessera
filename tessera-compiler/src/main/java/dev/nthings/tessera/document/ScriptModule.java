package dev.nthings.tessera.document;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/// A third-party ES module, such as a chart or date library, that a [PageScript] imports by
/// [#name]. Declare it with [PageScript#withImports(ScriptModule...)]. The compiler publishes the
/// source as a content-hashed file under `<assetBase>/modules/` on the site's own origin, and each
/// page that runs an importing script gets a `<script type="importmap">` that maps the name to that
/// file, so the script can write `import { format } from "date-fns";`.
///
/// A vendored module that imports another vendored module by name works when the page script
/// declares both. Two different modules with one name in one compilation are reported as
/// `script-module-conflict`.
///
/// @param name the bare import specifier, such as `chart` or `@scope/lib`: letters, digits, `.`,
///   `_`, `-` and `/`, not starting with `.` or `/`, with an optional leading `@scope/`
/// @param source the module's JavaScript source
public record ScriptModule(String name, String source) {

  private static final Pattern NAME =
      Pattern.compile(
          "(?:@[A-Za-z0-9_-][A-Za-z0-9._-]*/)?[A-Za-z0-9_-][A-Za-z0-9._-]*(?:/[A-Za-z0-9._-]+)*");

  /// Throws IllegalArgumentException unless `name` is a bare import specifier.
  public ScriptModule {
    if (!NAME.matcher(name).matches() || name.contains("..")) {
      throw new IllegalArgumentException("Script module name must be a bare specifier: " + name);
    }
  }

  /// The module `name` with `source`.
  public static ScriptModule of(String name, String source) {
    return new ScriptModule(name, source);
  }

  /// Reads `source` as UTF-8 now. Throws UncheckedIOException if the file cannot be read.
  public static ScriptModule file(String name, Path source) {
    try {
      return new ScriptModule(name, Files.readString(source, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read script module " + source, e);
    }
  }

  /// Loads the module source bundled as `resource`, resolved relative to the package of `anchor`.
  /// Throws IllegalArgumentException if the resource is missing.
  public static ScriptModule fromResource(String name, Class<?> anchor, String resource) {
    return new ScriptModule(name, PageScript.readResource(anchor, resource));
  }
}
