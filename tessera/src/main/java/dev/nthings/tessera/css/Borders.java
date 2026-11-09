package dev.nthings.tessera.css;

/// The [Border] implementations, built only through its static factories.
final class Borders {

  private Borders() {}

  /// A line of `width`, `style`, and `color`, such as `2px solid <color>`.
  record Line(CssLength width, Border.LineStyle style, CssColor color) implements Border {
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
}
