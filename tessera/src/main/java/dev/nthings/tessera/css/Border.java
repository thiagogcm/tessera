package dev.nthings.tessera.css;

/// A border or outline: a line, or `none`. Set it with [BoxStyled#border], one of its side-specific
/// border methods, or [BoxStyled#outline].
public sealed interface Border extends CssValue permits Border.Line, Border.None {

  /// Line style of a [Border.Line].
  enum LineStyle implements CssKeyword {
    SOLID,
    DASHED,
    DOTTED,
    DOUBLE
  }

  /// A line of `width`, `style`, and `color`, such as `2px solid <color>`.
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

  /// `none`: no border or outline.
  static Border none() {
    return None.INSTANCE;
  }

  /// `<width> <style> <color>`.
  static Border of(CssLength width, LineStyle style, CssColor color) {
    return new Line(width, style, color);
  }

  /// `<width> solid <color>`.
  static Border solid(CssLength width, CssColor color) {
    return new Line(width, LineStyle.SOLID, color);
  }

  /// `<width> dashed <color>`.
  static Border dashed(CssLength width, CssColor color) {
    return new Line(width, LineStyle.DASHED, color);
  }

  /// `1px solid <color>`.
  static Border hairline(CssColor color) {
    return solid(CssLength.px(1), color);
  }
}
