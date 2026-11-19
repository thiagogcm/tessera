package dev.nthings.tessera.core;

import java.io.Serializable;

/// One authoring error from expansion, linking, validation or site compilation. `rule` is a stable
/// kebab-case code, `message` says what is wrong, `fix` how to correct it, and `location` where it
/// happened.
public record Diagnostic(String rule, String message, String fix, Location location)
    implements Serializable {
  /// Formats the rule, contextual location, message and suggested correction.
  public String format() {
    var where = location.describe();
    return "[" + rule + "] " + (where.isEmpty() ? "" : where + ": ") + message + "\n  Fix: " + fix;
  }
}
