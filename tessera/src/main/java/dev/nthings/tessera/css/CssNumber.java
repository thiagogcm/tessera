package dev.nthings.tessera.css;

/// A unitless CSS number: a literal, a custom-property reference, a design token, or a tree
/// counting function. Build values with the static factories.
public sealed interface CssNumber extends CssValue
    permits CssNumbers.Literal, CssNumbers.Reference, CssNumbers.Sibling, DesignToken.Number {

  /// A literal number, such as `1.5`.
  static CssNumber of(double value) {
    return new CssNumbers.Literal(value);
  }

  /// `var(--name)` of a number-valued custom property. Throws IllegalArgumentException unless
  /// `customProperty` is dashed.
  static CssNumber property(CssIdent customProperty) {
    return new CssNumbers.Reference(customProperty);
  }

  /// `sibling-index()`, the 1-based position among siblings.
  static CssNumber siblingIndex() {
    return CssNumbers.Sibling.SIBLING_INDEX;
  }

  /// `sibling-count()`, the number of siblings including this element.
  static CssNumber siblingCount() {
    return CssNumbers.Sibling.SIBLING_COUNT;
  }
}
