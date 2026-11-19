package dev.nthings.tessera.view;

import dev.nthings.tessera.core.Location;

/// Backend-facing: where expansion and validation report authoring problems as diagnostics.
@FunctionalInterface
public interface DiagnosticReporter {

  /// Reports `rule` at `location`.
  void report(Location location, String rule, String message, String fix);
}
