package dev.nthings.tessera.core;

import java.util.List;

/// Attribute value made of the resolved DOM ids of `targets`, space-separated, after `prefix`
/// (e.g. `#` for fragment links).
public record IdReference(String prefix, List<ElementRef> targets) {

  public IdReference {
    if (targets.isEmpty()) {
      throw new IllegalArgumentException("IdReference requires at least one target");
    }
    targets = List.copyOf(targets);
  }
}
