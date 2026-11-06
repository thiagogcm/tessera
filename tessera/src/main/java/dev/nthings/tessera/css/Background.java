package dev.nthings.tessera.css;

/// A `background` of one soft-edged dot, positioned and sized, over a fill color.
///
/// Use [BoxStyled#bg(CssColor)] for a plain color and [BoxStyled#background(Background)] for a dot.
public record Background(
    Dot dot, CssLength x, CssLength y, CssLength width, CssLength height, CssColor fill)
    implements CssValue {

  /// `radial-gradient(circle closest-side, center solid%, transparent edge%)`.
  public record Dot(CssColor center, int solidPercent, int edgePercent) {
    String toCss() {
      return "radial-gradient(circle closest-side, "
          + center.toCss()
          + " "
          + solidPercent
          + "%, transparent "
          + edgePercent
          + "%)";
    }
  }

  @Override
  public String toCss() {
    return dot.toCss()
        + " "
        + x.toCss()
        + " "
        + y.toCss()
        + " / "
        + width.toCss()
        + " "
        + height.toCss()
        + " no-repeat "
        + fill.toCss();
  }
}
