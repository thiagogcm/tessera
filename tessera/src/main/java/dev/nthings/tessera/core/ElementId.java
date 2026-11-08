package dev.nthings.tessera.core;

import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.regex.Pattern;

/// Sanitized local id of a view boundary or [ElementRef], unique within its view scope.
///
/// `--` is reserved as the [ViewScope] path separator, so runs of `-` collapse to one.
public record ElementId(String slug) {
  private static final Pattern DASH_RUNS = Pattern.compile("-{2,}");

  public ElementId {
    slug = DASH_RUNS.matcher(Fnv1a64.sanitizeIdentifier(slug)).replaceAll("-");
  }

  public static ElementId of(String name) {
    return new ElementId(name);
  }
}
