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

  /// Label text for form fields and switches, buttons, and callout titles: the control font tokens
  /// at weight 600.
  public static final Typography LABEL =
      new Typography(
          Tokens.CONTROL_FAMILY,
          Tokens.CONTROL_FONT_SIZE,
          600,
          Tokens.CONTROL_LINE_HEIGHT,
          Tokens.CONTROL_TRACKING);

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
