package dev.nthings.tessera.core;

import java.util.Map;

/**
 * Declarative custom action contract that compiles to custom {@code --namespace-name} Invoker
 * Commands.
 */
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
