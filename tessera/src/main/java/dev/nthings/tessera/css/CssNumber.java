package dev.nthings.tessera.css;

/// A unitless CSS number: a literal, a custom-property reference, a design token, or a tree
/// counting function.
public sealed interface CssNumber extends CssValue
    permits CssNumber.Literal, CssNumber.Reference, CssNumber.Sibling, DesignToken.Number {

  /// A literal number.
  record Literal(double value) implements CssNumber {
    @Override
    public String toCss() {
      return CssLength.formatNumber(value);
    }
  }

  /// `var(--name)` of a custom property that holds a number.
  record Reference(CssIdent property) implements CssNumber {
    public Reference {
      property.requireCustom();
    }

    @Override
    public String toCss() {
      return property.varReference();
    }
  }

  /// Position functions evaluated per element among its siblings.
  enum Sibling implements CssNumber, CssKeyword {
    SIBLING_INDEX,
    SIBLING_COUNT;

    @Override
    public String toCss() {
      return CssKeyword.super.toCss() + "()";
    }
  }

  /// A literal number, such as `1.5`.
  static CssNumber of(double value) {
    return new Literal(value);
  }

  /// `var(--name)` of a number-valued custom property. Throws IllegalArgumentException unless
  /// `customProperty` is dashed.
  static CssNumber property(CssIdent customProperty) {
    return new Reference(customProperty);
  }

  /// `sibling-index()`, the 1-based position among siblings.
  static CssNumber siblingIndex() {
    return Sibling.SIBLING_INDEX;
  }

  /// `sibling-count()`, the number of siblings including this element.
  static CssNumber siblingCount() {
    return Sibling.SIBLING_COUNT;
  }
}
