package dev.nthings.tessera.core;

import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.Map;

/// Custom action that compiles to a `--namespace-name` Invoker Command.
public interface Action {

  String namespace();

  String name();

  default String toCustomCommand() {
    return "--"
        + Fnv1a64.sanitizeIdentifier(namespace())
        + "-"
        + Fnv1a64.sanitizeIdentifier(name());
  }

  default Map<String, String> payloadAttributes() {
    return Map.of();
  }
}
