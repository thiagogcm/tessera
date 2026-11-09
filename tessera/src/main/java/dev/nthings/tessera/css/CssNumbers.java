package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.Decimals;

/// The [CssNumber] implementations, built only through its static factories.
final class CssNumbers {

  private CssNumbers() {}

  /// A literal number.
  record Literal(double value) implements CssNumber {
    @Override
    public String toCss() {
      return Decimals.format(value);
    }
  }

  /// `var(--name)` of a custom property that holds a number.
  record Reference(CssIdent property) implements CssNumber {
    Reference {
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
}
