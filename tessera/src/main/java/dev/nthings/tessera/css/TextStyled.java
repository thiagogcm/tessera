package dev.nthings.tessera.css;

/// Typography, text color, alignment, wrapping, and decoration. Part of [Styled].
///
/// @param <SELF> concrete element or style builder type
interface TextStyled<SELF extends TextStyled<SELF>> extends StyleTarget<SELF> {

  /// Applies the font family, size, weight, line height, and tracking of `typography`.
  default SELF typography(Typography typography) {
    typography.apply(style());
    return self();
  }

  /// `color`, the text color. Use [CssColor#contrast()] for text on a filled background.
  default SELF textColor(CssColor color) {
    style().put(CssProperty.COLOR, color);
    return self();
  }

  /// `font-size`.
  default SELF fontSize(CssLength size) {
    style().put(CssProperty.FONT_SIZE, size);
    return self();
  }

  /// `font-family` set to a font family token.
  default SELF fontFamily(DesignToken.FontFamily family) {
    style().put(CssProperty.FONT_FAMILY, family);
    return self();
  }

  /// `font-family` set to a stack of families.
  default SELF fontFamily(FontStack stack) {
    style().put(CssProperty.FONT_FAMILY, stack);
    return self();
  }

  /// Takes the parent's font family.
  default SELF fontFamilyInherit() {
    style().put(CssProperty.FONT_FAMILY, "inherit");
    return self();
  }

  /// `font-weight`, such as 400 or 600.
  default SELF fontWeight(int weight) {
    style().put(CssProperty.FONT_WEIGHT, Integer.toString(weight));
    return self();
  }

  /// `font-variant-numeric: tabular-nums`: equal-width digits, so columns of numbers line up.
  default SELF tabularNums() {
    style().put(CssProperty.FONT_VARIANT_NUMERIC, "tabular-nums");
    return self();
  }

  /// A unitless line height, a multiple of the font size.
  default SELF lineHeight(double multiple) {
    return lineHeight(CssNumber.of(multiple));
  }

  /// A unitless line height from a number or number token.
  default SELF lineHeight(CssNumber multiple) {
    style().put(CssProperty.LINE_HEIGHT, multiple);
    return self();
  }

  /// `text-align`.
  default SELF textAlign(TextAlign alignment) {
    style().put(CssProperty.TEXT_ALIGN, alignment);
    return self();
  }

  /// `text-wrap`: [TextWrap#BALANCE] for headings, [TextWrap#PRETTY] for body text.
  default SELF textWrap(TextWrap wrap) {
    style().put(CssProperty.TEXT_WRAP, wrap);
    return self();
  }

  /// `white-space`.
  default SELF whiteSpace(WhiteSpace mode) {
    style().put(CssProperty.WHITE_SPACE, mode);
    return self();
  }

  /// `text-decoration-line: none`, such as for links styled as buttons.
  default SELF textDecorationNone() {
    style().put(CssProperty.TEXT_DECORATION_LINE, "none");
    return self();
  }

  /// Underlines at `offset` from the baseline; color and thickness come from the cascade.
  default SELF textDecorationUnderline(CssLength offset) {
    style().put(CssProperty.TEXT_DECORATION_LINE, "underline");
    style().put(CssProperty.TEXT_UNDERLINE_OFFSET, offset);
    return self();
  }

  /// Sets a highlight background, the tone's color mixed 24% over transparent (so it follows the
  /// light or dark scheme), with `padding-inline: var(--space-1)` and
  /// `border-radius: var(--radius-sm)`.
  default SELF highlight(Tone tone) {
    style().put(CssProperty.BACKGROUND, tone.color().mixTransparent(24));
    style().put(CssProperty.PADDING_INLINE, Tokens.SPACE_1);
    style().put(CssProperty.BORDER_RADIUS, Tokens.RADIUS_SM);
    return self();
  }

  /// A solid underline in the accent token, as `underline(UnderlineStyle.SOLID, <accent>)`.
  default SELF underline() {
    return underline(UnderlineStyle.SOLID, Tokens.ACCENT_PRIMARY);
  }

  /// Underlines in `color` with `lineStyle`, `0.1em` thick, offset `0.18em` from the baseline, with
  /// `text-decoration-skip-ink: auto`. [#textDecorationUnderline(CssLength)] sets only the offset,
  /// keeping the cascade's color and thickness.
  default SELF underline(UnderlineStyle lineStyle, CssColor color) {
    style().put(CssProperty.TEXT_DECORATION_LINE, "underline");
    style().put(CssProperty.TEXT_DECORATION_STYLE, lineStyle);
    style().put(CssProperty.TEXT_DECORATION_COLOR, color);
    style().put(CssProperty.TEXT_DECORATION_THICKNESS, CssLength.em(0.1));
    style().put(CssProperty.TEXT_UNDERLINE_OFFSET, CssLength.em(0.18));
    style().put(CssProperty.TEXT_DECORATION_SKIP_INK, "auto");
    return self();
  }

  /// A solid strikethrough in the muted text token.
  default SELF strikethrough() {
    return strikethrough(Tokens.TEXT_MUTED);
  }

  /// A solid strikethrough in `color`, `0.09em` thick.
  default SELF strikethrough(CssColor color) {
    style().put(CssProperty.TEXT_DECORATION_LINE, "line-through");
    style().put(CssProperty.TEXT_DECORATION_STYLE, UnderlineStyle.SOLID);
    style().put(CssProperty.TEXT_DECORATION_COLOR, color);
    style().put(CssProperty.TEXT_DECORATION_THICKNESS, CssLength.em(0.09));
    return self();
  }
}
