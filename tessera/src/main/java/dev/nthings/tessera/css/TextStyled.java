package dev.nthings.tessera.css;

import dev.nthings.tessera.internal.css.Tint;

/// Typography, text color, alignment, wrapping, and decoration. Part of [Styled].
///
/// @param <SELF> concrete element or style builder type
interface TextStyled<SELF extends TextStyled<SELF>> extends StyleTarget<SELF> {

  /// Applies the font family, size, weight, line height, and tracking of `typography`.
  default SELF typography(Typography typography) {
    typography.writeTo(StyleTarget.declarations(this));
    return self();
  }

  /// `color`, the text color. Use [CssColor#contrast()] for text on a filled background.
  default SELF textColor(CssColor color) {
    StyleTarget.declarations(this).put(CssProperty.COLOR, color);
    return self();
  }

  /// `font-size`.
  default SELF fontSize(CssLength size) {
    StyleTarget.declarations(this).put(CssProperty.FONT_SIZE, size);
    return self();
  }

  /// `font-family` set to a font family token.
  default SELF fontFamily(DesignToken.FontFamily family) {
    StyleTarget.declarations(this).put(CssProperty.FONT_FAMILY, family);
    return self();
  }

  /// `font-family` set to a stack of families.
  default SELF fontFamily(FontStack stack) {
    StyleTarget.declarations(this).put(CssProperty.FONT_FAMILY, stack);
    return self();
  }

  /// Takes the parent's font family.
  default SELF fontFamilyInherit() {
    StyleTarget.declarations(this).put(CssProperty.FONT_FAMILY, "inherit");
    return self();
  }

  /// `font-weight`, such as 400 or 600.
  default SELF fontWeight(int weight) {
    StyleTarget.declarations(this).put(CssProperty.FONT_WEIGHT, Integer.toString(weight));
    return self();
  }

  /// `font-variant-numeric: tabular-nums`: equal-width digits, so columns of numbers line up.
  default SELF tabularNums() {
    StyleTarget.declarations(this).put(CssProperty.FONT_VARIANT_NUMERIC, "tabular-nums");
    return self();
  }

  /// A unitless line height, a multiple of the font size.
  default SELF lineHeight(double multiple) {
    return lineHeight(CssNumber.of(multiple));
  }

  /// A unitless line height from a number or number token.
  default SELF lineHeight(CssNumber multiple) {
    StyleTarget.declarations(this).put(CssProperty.LINE_HEIGHT, multiple);
    return self();
  }

  /// `text-align`.
  default SELF textAlign(TextAlign alignment) {
    StyleTarget.declarations(this).put(CssProperty.TEXT_ALIGN, alignment);
    return self();
  }

  /// `text-wrap`: [TextWrap#BALANCE] for headings, [TextWrap#PRETTY] for body text.
  default SELF textWrap(TextWrap wrap) {
    StyleTarget.declarations(this).put(CssProperty.TEXT_WRAP, wrap);
    return self();
  }

  /// `white-space`.
  default SELF whiteSpace(WhiteSpace mode) {
    StyleTarget.declarations(this).put(CssProperty.WHITE_SPACE, mode);
    return self();
  }

  /// `text-decoration-line: none`, such as for links styled as buttons.
  default SELF textDecorationNone() {
    StyleTarget.declarations(this).put(CssProperty.TEXT_DECORATION_LINE, "none");
    return self();
  }

  /// Underlines at `offset` from the baseline; color and thickness come from the cascade.
  default SELF textDecorationUnderline(CssLength offset) {
    StyleTarget.declarations(this)
        .put(CssProperty.TEXT_DECORATION_LINE, "underline")
        .put(CssProperty.TEXT_UNDERLINE_OFFSET, offset);
    return self();
  }

  /// Sets a highlight background, a translucent tint of the tone's color (so it follows the light
  /// or dark scheme), with `padding-inline: var(--space-1)` and `border-radius: var(--radius-sm)`.
  default SELF highlight(Tone tone) {
    StyleTarget.declarations(this)
        .put(CssProperty.BACKGROUND, Tint.HIGHLIGHT.fill(tone))
        .put(CssProperty.PADDING_INLINE, Tokens.SPACE_1)
        .put(CssProperty.BORDER_RADIUS, Tokens.RADIUS_SM);
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
    StyleTarget.declarations(this)
        .put(CssProperty.TEXT_DECORATION_LINE, "underline")
        .put(CssProperty.TEXT_DECORATION_STYLE, lineStyle)
        .put(CssProperty.TEXT_DECORATION_COLOR, color)
        .put(CssProperty.TEXT_DECORATION_THICKNESS, CssLength.em(0.1))
        .put(CssProperty.TEXT_UNDERLINE_OFFSET, CssLength.em(0.18))
        .put(CssProperty.TEXT_DECORATION_SKIP_INK, "auto");
    return self();
  }

  /// A solid strikethrough in the muted text token.
  default SELF strikethrough() {
    return strikethrough(Tokens.TEXT_MUTED);
  }

  /// A solid strikethrough in `color`, `0.09em` thick.
  default SELF strikethrough(CssColor color) {
    StyleTarget.declarations(this)
        .put(CssProperty.TEXT_DECORATION_LINE, "line-through")
        .put(CssProperty.TEXT_DECORATION_STYLE, UnderlineStyle.SOLID)
        .put(CssProperty.TEXT_DECORATION_COLOR, color)
        .put(CssProperty.TEXT_DECORATION_THICKNESS, CssLength.em(0.09));
    return self();
  }
}
