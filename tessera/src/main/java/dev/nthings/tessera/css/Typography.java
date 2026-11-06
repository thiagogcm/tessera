package dev.nthings.tessera.css;

/// Font family, size, weight, line height, and tracking applied as separate declarations.
public record Typography(
    DesignToken.FontFamily family,
    DesignToken.Length size,
    int weight,
    DesignToken.Number lineHeight,
    DesignToken.Length tracking) {

  public StyleRefinement apply(StyleRefinement style) {
    return style
        .put(CssProperty.FONT_FAMILY, family.varReference())
        .put(CssProperty.FONT_SIZE, size.length())
        .put(CssProperty.FONT_WEIGHT, Integer.toString(weight))
        .put(CssProperty.LINE_HEIGHT, lineHeight.varReference())
        .put(CssProperty.LETTER_SPACING, tracking.length());
  }
}
