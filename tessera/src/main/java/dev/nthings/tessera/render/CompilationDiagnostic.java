package dev.nthings.tessera.render;

import java.io.Serializable;

/// One authoring error found during compilation. `rule` is a stable kebab-case code, such as
/// `image-alt` or `unbound-reference`. The accessibility rule codes are described in the
/// architecture notes (`docs/architecture.md`). `suggestion` is the fix text.
public record CompilationDiagnostic(
    String rule, CompilationLocation location, String message, String suggestion)
    implements Serializable {

  /// `[rule] location: message` followed by the suggested fix.
  public String format() {
    return "[" + rule + "] " + location.describe() + ": " + message + "\n  Fix: " + suggestion;
  }

  /// Compiler-facing: returns this diagnostic with its location attached to route `path`.
  public CompilationDiagnostic atRoute(String path) {
    return new CompilationDiagnostic(rule, location.atRoute(path), message, suggestion);
  }
}
