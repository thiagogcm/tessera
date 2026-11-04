package dev.nthings.tessera.core;

/// Sanitized local identifier for a view boundary or an [ElementRef], unique within its view scope.
///
/// `--` is reserved as the [ViewScope] path separator, so runs of `-` collapse to one.
public record ElementId(String slug) {

  public ElementId {
    slug = Fnv1a64.sanitizeIdentifier(slug).replaceAll("-{2,}", "-");
  }

  public static ElementId of(String name) {
    return new ElementId(name);
  }
}
