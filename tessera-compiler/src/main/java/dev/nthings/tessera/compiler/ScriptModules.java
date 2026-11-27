package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.document.PageScript;
import dev.nthings.tessera.document.ScriptModule;
import dev.nthings.tessera.internal.foundation.Fnv1a64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Publishes the vendored [ScriptModule]s that page scripts import, and writes the import map that
/// resolves their names.
final class ScriptModules {
  private ScriptModules() {}

  /// The base vendored modules publish under: the external asset base, or the default one inline,
  /// since an import map can only point at files.
  static String base(OutputMode output) {
    return switch (output) {
      case OutputMode.External(var base) -> base;
      case OutputMode.Inline() -> OutputMode.DEFAULT_EXTERNAL_BASE;
    };
  }

  /// `<base>/modules/<sanitized name>-<content hash>.js`.
  static String path(String base, ScriptModule module) {
    return HtmlWriter.hashedPath(
        base + "/modules", Fnv1a64.sanitizeIdentifier(module.name()), module.source(), ".js");
  }

  /// The modules `scripts` import, each name once, in first-import order.
  static List<ScriptModule> imported(Iterable<PageScript> scripts) {
    var byName = new LinkedHashMap<String, ScriptModule>();
    for (var script : scripts) {
      for (var module : script.imports()) {
        byName.putIfAbsent(module.name(), module);
      }
    }
    return List.copyOf(byName.values());
  }

  /// Reports each name that `scripts` bind to more than one source.
  static List<Diagnostic> conflicts(Iterable<PageScript> scripts) {
    var byName = new LinkedHashMap<String, ScriptModule>();
    var reported = new LinkedHashMap<String, Diagnostic>();
    for (var script : scripts) {
      for (var module : script.imports()) {
        var first = byName.putIfAbsent(module.name(), module);
        if (first != null && !first.equals(module) && !reported.containsKey(module.name())) {
          reported.put(
              module.name(),
              new Diagnostic(
                  "script-module-conflict",
                  "Script module \""
                      + module.name()
                      + "\" is declared with different sources (page script \""
                      + script.id()
                      + "\")",
                  "Import one source under each module name, or give the other module its own name.",
                  Location.ROOT));
        }
      }
    }
    return List.copyOf(reported.values());
  }

  /// The JSON import map mapping each module `scripts` import to its path under `base`, or null
  /// when they import none.
  static @Nullable String importMap(Iterable<PageScript> scripts, String base) {
    var modules = imported(scripts);
    if (modules.isEmpty()) {
      return null;
    }
    var json = new StringBuilder("{\"imports\":{");
    for (var i = 0; i < modules.size(); i++) {
      var module = modules.get(i);
      if (i > 0) {
        json.append(',');
      }
      json.append('"')
          .append(SiteArtifacts.escapeJson(module.name()))
          .append("\":\"")
          .append(SiteArtifacts.escapeJson(path(base, module)))
          .append('"');
    }
    return json.append("}}").toString();
  }

  /// The CSP source allowing the inline script whose text is `script`.
  static String cspHash(String script) {
    try {
      var digest =
          MessageDigest.getInstance("SHA-256").digest(script.getBytes(StandardCharsets.UTF_8));
      return "'sha256-" + Base64.getEncoder().encodeToString(digest) + "'";
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
