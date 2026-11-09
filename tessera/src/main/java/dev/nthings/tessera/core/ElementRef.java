package dev.nthings.tessera.core;

import dev.nthings.tessera.foundation.Fnv1a64;
import org.jspecify.annotations.Nullable;

/// Identity handle for an element's DOM id, resolved by the compiler once the page is expanded.
/// It is the only way to give an element an id.
///
/// Bind a ref with [TagElement#id(ElementRef)]. Elements may reference it through `for`,
/// `commandfor`, `aria-*` or `href`, before or after the bind site. Anchor positioning is the
/// exception: [TagElement#anchoredTo] needs its anchor bound earlier on the page, or the compiler
/// reports `unbound-reference`. Several refs may name one element: they alias the same DOM id.
/// Binding one ref to two elements on a page reports `duplicate-reference-binding`, but a ref can
/// be reused across pages. Equality is identity, so two refs with the same name are different
/// handles.
///
/// Scoped names are sanitized: characters outside `[A-Za-z0-9_-]` become `-` and runs of `-`
/// collapse to one. Page names are kept as written. An empty name throws IllegalArgumentException.
public final class ElementRef {

  private final @Nullable String name;
  private final boolean pageLevel;

  private ElementRef(@Nullable String name, boolean pageLevel) {
    this.name = name;
    this.pageLevel = pageLevel;
  }

  /// Creates a ref whose DOM id is `name` prefixed by the bind site's id scope. Inside an id
  /// scope with id `toolbar`, `scoped("save")` emits `toolbar--save`. At the top of the page it
  /// emits `save`. Two refs with the same name bound in one scope report `duplicate-id`.
  public static ElementRef scoped(String name) {
    return new ElementRef(Fnv1a64.sanitizeLocalId(name), false);
  }

  /// Creates a ref whose DOM id is `name` exactly as written, wherever it is bound. Use it for
  /// stable public fragments such as `/guide/#installation`; any characters but whitespace are
  /// allowed, so `café` links as `#caf%C3%A9`. The id must be unique on the page, or the compiler
  /// reports `duplicate-id`. Throws IllegalArgumentException if `name` is empty or contains
  /// whitespace.
  public static ElementRef page(String name) {
    if (name.isEmpty() || name.chars().anyMatch(Character::isWhitespace)) {
      throw new IllegalArgumentException(
          "Page-level id must be non-empty and contain no whitespace: '" + name + "'");
    }
    return new ElementRef(name, true);
  }

  /// Creates a ref whose local id the compiler assigns as `<tag>-<n>`, numbered per id scope.
  /// When the element also has a named ref, it takes that ref's id instead.
  public static ElementRef anonymous() {
    return new ElementRef(null, false);
  }

  /// Compiler-facing: the name (sanitized when scoped), or `null` for an anonymous ref.
  public @Nullable String name() {
    return name;
  }

  /// Compiler-facing: whether the name is used as is, without the id scope prefix.
  public boolean pageLevel() {
    return pageLevel;
  }

  @Override
  public String toString() {
    if (name == null) {
      return "ElementRef(anonymous)";
    }
    return pageLevel ? "ElementRef.page(" + name + ")" : "ElementRef.scoped(" + name + ")";
  }
}
