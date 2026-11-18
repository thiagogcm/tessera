package dev.nthings.tessera.internal.authoring;

import dev.nthings.tessera.core.ElementRef;
import java.util.List;

/// Compiler-facing: DOM ids of `targets` for an id-reference attribute such as `for`, `list`,
/// `href` or `commandfor`. Authors set these through typed methods such as `TagElement.labelFor`.
/// The ids are joined with spaces and written once after `prefix`: `#` for `href` gives `#a b` for
/// two targets. An unbound target is reported as `unbound-reference`, and the attribute is omitted.
public record IdReference(String prefix, List<ElementRef> targets) {

  /// Throws IllegalArgumentException if `targets` is empty.
  public IdReference {
    if (targets.isEmpty()) {
      throw new IllegalArgumentException("IdReference requires at least one target");
    }
    targets = List.copyOf(targets);
  }
}
