package dev.nthings.tessera.core;

import java.util.List;

/// DOM ids of `targets` for an id-reference attribute such as `for`, `list`, `href` or
/// `commandfor`. The ids are joined with spaces and written once after `prefix`: `#` for `href`
/// gives `#a b` for two targets. An unbound target is reported as `unbound-reference`, and the
/// attribute is omitted.
public record IdReference(String prefix, List<ElementRef> targets) {

  /// Throws IllegalArgumentException if `targets` is empty.
  public IdReference {
    if (targets.isEmpty()) {
      throw new IllegalArgumentException("IdReference requires at least one target");
    }
    targets = List.copyOf(targets);
  }
}
