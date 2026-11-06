package dev.nthings.tessera.core;

import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.Map;

/// Declarative custom action contract that compiles to custom `--namespace-name` Invoker
/// Commands.
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
