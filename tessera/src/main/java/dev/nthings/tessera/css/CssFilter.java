package dev.nthings.tessera.css;

/// A filter function for `backdrop-filter`.
public sealed interface CssFilter extends CssValue permits CssFilter.Blur {

  /// `blur(radius)`.
  record Blur(CssLength radius) implements CssFilter {
    @Override
    public String toCss() {
      return "blur(" + radius.toCss() + ")";
    }
  }

  static CssFilter blur(CssLength radius) {
    return new Blur(radius);
  }
}
