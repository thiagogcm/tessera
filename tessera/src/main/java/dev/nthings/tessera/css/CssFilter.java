package dev.nthings.tessera.css;

/// A filter function for `backdrop-filter`, set with [EffectStyled#backdropFilter].
public sealed interface CssFilter extends CssValue permits CssFilter.Blur {

  /// `blur(radius)`.
  record Blur(CssLength radius) implements CssFilter {
    @Override
    public String toCss() {
      return "blur(" + radius.toCss() + ")";
    }
  }

  /// `blur(radius)`, such as `blur(4px)`.
  static CssFilter blur(CssLength radius) {
    return new Blur(radius);
  }
}
