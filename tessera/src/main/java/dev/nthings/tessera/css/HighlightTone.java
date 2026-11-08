package dev.nthings.tessera.css;

/// Highlight tones: `color-mix(in oklch, var(--token) 24%, transparent)`, which follows
/// the `light-dark()` theme.
public enum HighlightTone {
  ACCENT(Tokens.ACCENT_PRIMARY),
  WARNING(Tokens.STATUS_WARNING),
  SUCCESS(Tokens.STATUS_SUCCESS),
  DANGER(Tokens.STATUS_DANGER),
  INFO(Tokens.STATUS_INFO);

  private final DesignToken.Color token;

  HighlightTone(DesignToken.Color token) {
    this.token = token;
  }

  public DesignToken.Color token() {
    return token;
  }

  public CssColor background() {
    return token.mixTransparent(24);
  }
}
