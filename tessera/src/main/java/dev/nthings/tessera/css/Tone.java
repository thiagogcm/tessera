package dev.nthings.tessera.css;

/// The semantic color of a component or highlight, shared by `Button`, `Badge`,
/// [Styled#highlight(Tone)] and others. Each component decides how a tone paints (fill, text,
/// border); [#color()], [#textColor()] and [#onSolid()] are the colors it draws from.
public enum Tone {
  NEUTRAL(Tokens.TEXT_MUTED, Tokens.TEXT_MUTED, Tokens.SURFACE_CARD),
  ACCENT(Tokens.ACCENT_PRIMARY, Tokens.ACCENT_TEXT, onFill(262)),
  INFO(Tokens.STATUS_INFO, Tokens.STATUS_INFO_TEXT, onFill(235)),
  SUCCESS(Tokens.STATUS_SUCCESS, Tokens.STATUS_SUCCESS_TEXT, onFill(152)),
  WARNING(Tokens.STATUS_WARNING, Tokens.STATUS_WARNING_TEXT, CssColor.oklch(15, 0.02, 75)),
  DANGER(Tokens.STATUS_DANGER, Tokens.STATUS_DANGER_TEXT, onFill(25));

  private final DesignToken.Color color;
  private final DesignToken.Color textColor;
  private final CssColor onSolid;

  Tone(DesignToken.Color color, DesignToken.Color textColor, CssColor onSolid) {
    this.color = color;
    this.textColor = textColor;
    this.onSolid = onSolid;
  }

  /// White on the darker light-mode fills; near-black in dark mode, whose fills are lighter.
  private static CssColor onFill(double hue) {
    return CssColor.lightDark(CssColor.white(), CssColor.oklch(18, 0.02, hue));
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

  /// The foreground color on a solid fill of the tone, such as the label of a solid button or
  /// badge.
  public CssColor onSolid() {
    return onSolid;
  }
}
