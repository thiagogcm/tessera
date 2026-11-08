package dev.nthings.tessera.core;

import org.jspecify.annotations.Nullable;

/// Linker-style handle for an element's DOM id, resolved by the compiler.
///
/// Bind once per page with [TagElement#id(ElementRef)]. The DOM id is the bind site's [ViewScope]
/// plus [#localId()]. Any element may reference it (`commandfor`, `for`, `aria-*`, `href="#…"`,
/// CSS anchors), before or after the bind site. Equality is identity.
public final class ElementRef {

  private final @Nullable ElementId localId;

  private ElementRef(@Nullable ElementId localId) {
    this.localId = localId;
  }

  /// Ref whose DOM id ends in `name` (sanitized) within the bind site's view scope.
  public static ElementRef named(String name) {
    return new ElementRef(ElementId.of(name));
  }

  /// Ref whose local id is assigned by the compiler (`<tag>-<n>`, numbered per view scope).
  public static ElementRef anonymous() {
    return new ElementRef(null);
  }

  public @Nullable ElementId localId() {
    return localId;
  }

  @Override
  public String toString() {
    return localId != null ? "ElementRef(" + localId.slug() + ")" : "ElementRef(anonymous)";
  }
}
