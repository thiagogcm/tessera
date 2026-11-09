package dev.nthings.tessera.css;

/// The semantic color of a component or highlight, shared by `Button`, `Badge`,
/// [Styled#highlight(Tone)] and others. Each component decides how a tone paints (fill, text,
/// border); [#color()] and [#textColor()] are the theme tokens it draws from.
public enum Tone {
  NEUTRAL(Tokens.TEXT_MUTED, Tokens.TEXT_MUTED),
  ACCENT(Tokens.ACCENT_PRIMARY, Tokens.ACCENT_TEXT),
  INFO(Tokens.STATUS_INFO, Tokens.STATUS_INFO_TEXT),
  SUCCESS(Tokens.STATUS_SUCCESS, Tokens.STATUS_SUCCESS_TEXT),
  WARNING(Tokens.STATUS_WARNING, Tokens.STATUS_WARNING_TEXT),
  DANGER(Tokens.STATUS_DANGER, Tokens.STATUS_DANGER_TEXT);

  private final DesignToken.Color color;
  private final DesignToken.Color textColor;

  Tone(DesignToken.Color color, DesignToken.Color textColor) {
    this.color = color;
    this.textColor = textColor;
  }

  /// The tone's base color token, for fills, borders and icons.
  public DesignToken.Color color() {
    return color;
  }

  /// The tone's color for small text on tinted or canvas surfaces, darker or lighter than
  /// [#color()] so it stays readable.
  public DesignToken.Color textColor() {
    return textColor;
  }
}
