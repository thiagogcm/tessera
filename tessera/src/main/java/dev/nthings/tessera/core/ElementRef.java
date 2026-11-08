package dev.nthings.tessera.core;

import org.jspecify.annotations.Nullable;

/// Identity handle for an element's DOM id, resolved by the compiler like a linker symbol.
///
/// Bind it once per page with [TagElement#id(ElementRef)]; its DOM id is the enclosing
/// [ViewScope] at the bind site extended by [#localId()]. Any number of elements may reference it
/// (`commandfor`, `for`, `aria-*`, `href="#…"`, CSS anchors) from anywhere on the page, before or
/// after the bind site. Two refs are equal only if they are the same instance.
public final class ElementRef {

  private final @Nullable ElementId localId;

  private ElementRef(@Nullable ElementId localId) {
    this.localId = localId;
  }

  /// Ref whose DOM id ends in `name` (sanitized) within the bind site's view scope.
  public static ElementRef named(String name) {
    return new ElementRef(ElementId.of(name));
  }

  /// Ref whose local id is assigned by the compiler (`<tag>-<n>`, numbered within its
  /// view scope, so ids elsewhere on the page never shift it).
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
