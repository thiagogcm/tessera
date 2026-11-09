package dev.nthings.tessera.css;

/// A border or outline: a line, or `none`. Set it with [Styled#border(Border)], one of its
/// side-specific border methods, or [Styled#outline(Border)]. Build values with the static
/// factories.
public sealed interface Border extends CssValue permits Borders.Line, Borders.None {

  /// Line style of a border, such as `solid`.
  enum LineStyle implements CssKeyword {
    SOLID,
    DASHED,
    DOTTED,
    DOUBLE
  }

  /// `none`: no border or outline.
  static Border none() {
    return Borders.None.INSTANCE;
  }

  /// `<width> <style> <color>`.
  static Border of(CssLength width, LineStyle style, CssColor color) {
    return new Borders.Line(width, style, color);
  }

  /// `<width> solid <color>`.
  static Border solid(CssLength width, CssColor color) {
    return of(width, LineStyle.SOLID, color);
  }

  /// `<width> dashed <color>`.
  static Border dashed(CssLength width, CssColor color) {
    return of(width, LineStyle.DASHED, color);
  }

  /// `1px solid <color>`, the hairline used for card and divider borders.
  static Border hairline(CssColor color) {
    return solid(CssLength.px(1), color);
  }
}
