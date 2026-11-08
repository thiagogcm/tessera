package dev.nthings.tessera.core;

import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.Map;

/// Custom action sent to a target with [InteractiveElement#dispatchAction], compiled to a
/// `--namespace-name` Invoker Command. Each part is sanitized: characters outside `[A-Za-z0-9_-]`
/// become `-`, and an empty part throws IllegalArgumentException when the command is built.
/// Custom commands are checked only for an existing target (`missing-command-target`).
public interface Action {

  /// The namespace part of the command name, before sanitizing.
  String namespace();

  /// The action part of the command name, before sanitizing.
  String name();

  /// Returns the `command` attribute value, `--` followed by the sanitized namespace and name.
  default String toCustomCommand() {
    return "--"
        + Fnv1a64.sanitizeIdentifier(namespace())
        + "-"
        + Fnv1a64.sanitizeIdentifier(name());
  }

  /// Returns attributes set on the invoking element together with the action, such as `data-id`.
  /// Keys `role` and `aria-*` are refused with IllegalArgumentException.
  default Map<String, String> payloadAttributes() {
    return Map.of();
  }
}
