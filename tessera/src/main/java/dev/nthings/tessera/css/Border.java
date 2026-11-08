package dev.nthings.tessera.css;

/// A border or outline: a line, or `none`.
public sealed interface Border extends CssValue permits Border.Line, Border.None {

  enum LineStyle implements CssKeyword {
    SOLID,
    DASHED,
    DOTTED,
    DOUBLE
  }

  record Line(CssLength width, LineStyle style, CssColor color) implements Border {
    @Override
    public String toCss() {
      return width.toCss() + " " + style.toCss() + " " + color.toCss();
    }
  }

  /// `none`.
  enum None implements Border {
    INSTANCE;

    @Override
    public String toCss() {
      return "none";
    }
  }

  static Border none() {
    return None.INSTANCE;
  }

  static Border of(CssLength width, LineStyle style, CssColor color) {
    return new Line(width, style, color);
  }

  static Border solid(CssLength width, CssColor color) {
    return new Line(width, LineStyle.SOLID, color);
  }

  static Border dashed(CssLength width, CssColor color) {
    return new Line(width, LineStyle.DASHED, color);
  }

  /// A 1px solid line.
  static Border hairline(CssColor color) {
    return solid(CssLength.px(1), color);
  }
}
