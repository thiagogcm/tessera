package dev.nthings.tessera.css;

/// A filter function for `backdrop-filter`, set with [Styled#backdropFilter(CssFilter)]. Build
/// values with the static factories.
public sealed interface CssFilter extends CssValue permits CssFilters.Blur {

  /// `blur(radius)`, such as `blur(4px)`.
  static CssFilter blur(CssLength radius) {
    return new CssFilters.Blur(radius);
  }
}
