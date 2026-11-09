package dev.nthings.tessera.css;

/// The [CssFilter] implementations, built only through its static factories.
final class CssFilters {

  private CssFilters() {}

  /// `blur(radius)`.
  record Blur(CssLength radius) implements CssFilter {
    @Override
    public String toCss() {
      return "blur(" + radius.toCss() + ")";
    }
  }
}
