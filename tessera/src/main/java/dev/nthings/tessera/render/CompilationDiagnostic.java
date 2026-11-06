package dev.nthings.tessera.render;

import java.io.Serializable;

/// One authoring error, identified by a rule code and accompanied by a suggested correction.
public record CompilationDiagnostic(
    String rule, CompilationLocation location, String message, String suggestion)
    implements Serializable {

  public CompilationDiagnostic atRoute(String path) {
    return new CompilationDiagnostic(rule, location.atRoute(path), message, suggestion);
  }
}
