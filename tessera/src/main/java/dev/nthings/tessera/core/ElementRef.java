package dev.nthings.tessera.core;

import org.jspecify.annotations.Nullable;

/// Identity handle for an element's DOM id, resolved by the compiler once the page is expanded.
///
/// Bind a ref once per page with [TagElement#id(ElementRef)], or with [TagElement#ref()] for an
/// anonymous ref. The DOM id is the bind site's view scope followed by the local id. Elements may
/// reference a bound ref through `for`, `commandfor`, `aria-*` or `href`, before or after the bind
/// site. Anchor positioning is the exception: [TagElement#anchoredTo] needs its anchor bound
/// earlier on the page, or the compiler reports `unbound-reference`. Binding a ref twice on a page
/// reports `duplicate-reference-binding`, but refs can be reused across pages. Equality is
/// identity.
public final class ElementRef {

  private final @Nullable ElementId localId;

  private ElementRef(@Nullable ElementId localId) {
    this.localId = localId;
  }

  /// Creates a ref with the local id `name`, sanitized as [ElementId] does, in the bind site's view
  /// scope. Two bound refs with the same name in one view scope report `duplicate-id`.
  public static ElementRef named(String name) {
    return new ElementRef(ElementId.of(name));
  }

  /// Creates a ref whose local id the compiler assigns as `<tag>-<n>`, numbered per view scope.
  public static ElementRef anonymous() {
    return new ElementRef(null);
  }

  /// Compiler-facing: the local id this ref asks for, or `null` when the compiler assigns one.
  public @Nullable ElementId localId() {
    return localId;
  }

  @Override
  public String toString() {
    return localId != null ? "ElementRef(" + localId.slug() + ")" : "ElementRef(anonymous)";
  }
}
