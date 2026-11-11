package dev.nthings.tessera.compiler;

import java.io.Serializable;

/// One authoring error found during compilation. `rule` is a stable kebab-case code, such as
/// `image-alt`, `unbound-reference` or `duplicate-route`. `message` says what is wrong, `fix` how
/// to correct it, and `location` where it happened.
public record Diagnostic(String rule, String message, String fix, Location location)
    implements Serializable {

  /// `[rule] location: message` followed by the fix, as [CompilationException] prints it.
  public String format() {
    var where = location.describe();
    return "[" + rule + "] " + (where.isEmpty() ? "" : where + ": ") + message + "\n  Fix: " + fix;
  }

  /// Returns this diagnostic with its location attached to page `title`.
  Diagnostic atPage(String title) {
    return new Diagnostic(rule, message, fix, location.atPage(title));
  }

  /// Returns this diagnostic with its location attached to route `path`.
  Diagnostic atRoute(String path) {
    return new Diagnostic(rule, message, fix, location.atRoute(path));
  }
}
