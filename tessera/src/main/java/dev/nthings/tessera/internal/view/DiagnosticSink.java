package dev.nthings.tessera.internal.view;

import dev.nthings.tessera.core.Location;

/// Internal sink where shared expansion and validation report authoring problems as diagnostics.
@FunctionalInterface
public interface DiagnosticSink {

  /// Reports `rule` at `location`.
  void report(Location location, String rule, String message, String fix);
}
