package dev.nthings.tessera.core;

import java.util.List;

/// Space-separated DOM ids of `targets`, each after `prefix` (e.g. `#`).
public record IdReference(String prefix, List<ElementRef> targets) {

  public IdReference {
    if (targets.isEmpty()) {
      throw new IllegalArgumentException("IdReference requires at least one target");
    }
    targets = List.copyOf(targets);
  }
}
