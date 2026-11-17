package dev.nthings.tessera.css;

/// Font family, size, weight, line height, and tracking, applied with
/// [Styled#typography(Typography)]. Every field is a design token except `weight`, a plain font
/// weight.
public record Typography(
    DesignToken.FontFamily family,
    DesignToken.Length size,
    int weight,
    DesignToken.Number lineHeight,
    DesignToken.Length tracking) {

  /// Writes the five declarations to `style`; [Styled#typography(Typography)] calls it.
  void writeTo(Declarations style) {
    style
        .put(CssProperty.FONT_FAMILY, family)
        .put(CssProperty.FONT_SIZE, size)
        .put(CssProperty.FONT_WEIGHT, Integer.toString(weight))
        .put(CssProperty.LINE_HEIGHT, lineHeight)
        .put(CssProperty.LETTER_SPACING, tracking);
  }
}
