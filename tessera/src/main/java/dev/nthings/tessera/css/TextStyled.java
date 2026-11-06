package dev.nthings.tessera.css;

/// Typography, text color, alignment, wrapping, and decoration.
///
/// @param <SELF> concrete element or style builder type
public interface TextStyled<SELF extends TextStyled<SELF>> extends StyleTarget<SELF> {

  default SELF typography(Typography typography) {
    typography.apply(style());
    return self();
  }

  default SELF textColor(CssColor color) {
    style().put(CssProperty.COLOR, color);
    return self();
  }

  /// Text color that contrasts with `background` (`contrast-color()`), e.g. for filled buttons.
  default SELF textColorOn(CssColor background) {
    return textColor(background.contrast());
  }

  default SELF fontSize(CssLength size) {
    style().put(CssProperty.FONT_SIZE, size);
    return self();
  }

  default SELF fontFamily(DesignToken.FontFamily family) {
    style().put(CssProperty.FONT_FAMILY, family);
    return self();
  }

  default SELF fontFamily(FontStack stack) {
    style().put(CssProperty.FONT_FAMILY, stack);
    return self();
  }

  /// Takes the parent's font family.
  default SELF fontFamilyInherit() {
    style().put(CssProperty.FONT_FAMILY, "inherit");
    return self();
  }

  default SELF fontWeight(int weight) {
    style().put(CssProperty.FONT_WEIGHT, Integer.toString(weight));
    return self();
  }

  default SELF tabularNums() {
    style().put(CssProperty.FONT_VARIANT_NUMERIC, "tabular-nums");
    return self();
  }

  /// A unitless line height, a multiple of the font size.
  default SELF lineHeight(double multiple) {
    return lineHeight(CssNumber.of(multiple));
  }

  default SELF lineHeight(CssNumber multiple) {
    style().put(CssProperty.LINE_HEIGHT, multiple);
    return self();
  }

  default SELF textAlign(TextAlign alignment) {
    style().put(CssProperty.TEXT_ALIGN, alignment);
    return self();
  }

  default SELF textBalance() {
    style().put(CssProperty.TEXT_WRAP, "balance");
    return self();
  }

  default SELF textPretty() {
    style().put(CssProperty.TEXT_WRAP, "pretty");
    return self();
  }

  default SELF whiteSpace(WhiteSpace mode) {
    style().put(CssProperty.WHITE_SPACE, mode);
    return self();
  }

  default SELF textDecorationNone() {
    style().put(CssProperty.TEXT_DECORATION_LINE, "none");
    return self();
  }

  /// Underlines the text at `offset` from the baseline, leaving color and thickness to the cascade.
  default SELF textDecorationUnderline(CssLength offset) {
    style().put(CssProperty.TEXT_DECORATION_LINE, "underline");
    style().put(CssProperty.TEXT_UNDERLINE_OFFSET, offset);
    return self();
  }

  default SELF highlight(HighlightTone tone) {
    style().put(CssProperty.BACKGROUND, tone.background());
    style().put(CssProperty.PADDING_INLINE, Tokens.SPACE_1);
    style().put(CssProperty.BORDER_RADIUS, Tokens.RADIUS_SM);
    return self();
  }

  default SELF underline() {
    return underline(UnderlineStyle.SOLID, Tokens.ACCENT_PRIMARY);
  }

  default SELF underline(UnderlineStyle lineStyle, CssColor color) {
    style().put(CssProperty.TEXT_DECORATION_LINE, "underline");
    style().put(CssProperty.TEXT_DECORATION_STYLE, lineStyle);
    style().put(CssProperty.TEXT_DECORATION_COLOR, color);
    style().put(CssProperty.TEXT_DECORATION_THICKNESS, CssLength.em(0.1));
    style().put(CssProperty.TEXT_UNDERLINE_OFFSET, CssLength.em(0.18));
    style().put(CssProperty.TEXT_DECORATION_SKIP_INK, "auto");
    return self();
  }

  default SELF strikethrough() {
    return strikethrough(Tokens.TEXT_MUTED);
  }

  default SELF strikethrough(CssColor color) {
    style().put(CssProperty.TEXT_DECORATION_LINE, "line-through");
    style().put(CssProperty.TEXT_DECORATION_STYLE, UnderlineStyle.SOLID);
    style().put(CssProperty.TEXT_DECORATION_COLOR, color);
    style().put(CssProperty.TEXT_DECORATION_THICKNESS, CssLength.em(0.09));
    return self();
  }
}
