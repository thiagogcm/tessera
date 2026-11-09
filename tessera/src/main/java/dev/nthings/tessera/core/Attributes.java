package dev.nthings.tessera.core;

/// Checks shared by the generic attribute setters.
final class Attributes {
  private Attributes() {}

  /// Refuses `id`, `role` and `aria-*`, which have typed methods.
  static void requireGeneric(String attribute) {
    if (attribute.equals("id")) {
      throw new IllegalArgumentException(
          "Use id(ElementRef) instead of the attribute 'id', so the compiler can scope and link it");
    }
    if (attribute.equals("role") || attribute.startsWith("aria-")) {
      throw new IllegalArgumentException(
          "Use the typed Accessible method (role(Role), aria...(...)) instead of the attribute '"
              + attribute
              + "'");
    }
  }
}
