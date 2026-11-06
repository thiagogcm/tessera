package dev.nthings.tessera.css;

/// Semantic highlight color tones backed by `color-mix(in oklch, var(--token) 24%, transparent)`
/// so highlights adapt automatically to `light-dark()` themes.
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

  public String toCssBackground() {
    return token.mixTransparent(24);
  }
}
