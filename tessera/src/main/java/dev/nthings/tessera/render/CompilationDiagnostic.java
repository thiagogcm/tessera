package dev.nthings.tessera.render;

import java.io.Serializable;

public record CompilationDiagnostic(
    String rule, CompilationLocation location, String message, String suggestion)
    implements Serializable {

  /// `[rule] location: message` followed by the suggested fix.
  public String format() {
    return "[" + rule + "] " + location.describe() + ": " + message + "\n  Fix: " + suggestion;
  }

  public CompilationDiagnostic atRoute(String path) {
    return new CompilationDiagnostic(rule, location.atRoute(path), message, suggestion);
  }
}
