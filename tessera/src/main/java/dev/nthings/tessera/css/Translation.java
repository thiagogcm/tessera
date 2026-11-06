package dev.nthings.tessera.css;

/// The `translate` property: a horizontal and a vertical offset.
public record Translation(CssLength x, CssLength y) implements CssValue {

  public static Translation of(CssLength x, CssLength y) {
    return new Translation(x, y);
  }

  /// No offset, the resting position of an entry transition.
  public static Translation none() {
    return new Translation(CssLength.zero(), CssLength.zero());
  }

  /// A vertical offset only.
  public static Translation y(CssLength y) {
    return new Translation(CssLength.zero(), y);
  }

  @Override
  public String toCss() {
    return x.toCss() + " " + y.toCss();
  }
}
