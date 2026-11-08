package dev.nthings.tessera.core;

import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.regex.Pattern;

/// Sanitized local id of a view boundary or [ElementRef], unique within its view scope.
///
/// Characters outside `[A-Za-z0-9_-]` become `-`, and an empty name throws
/// IllegalArgumentException.
/// `--` is the separator of view scope paths, so runs of `-` collapse to one. Uniqueness within a
/// view scope is required: two elements with the same id report `duplicate-id`.
public record ElementId(String slug) {
  private static final Pattern DASH_RUNS = Pattern.compile("-{2,}");

  public ElementId {
    slug = DASH_RUNS.matcher(Fnv1a64.sanitizeIdentifier(slug)).replaceAll("-");
  }

  /// Creates an id from `name`, sanitized as described on this type.
  public static ElementId of(String name) {
    return new ElementId(name);
  }
}
