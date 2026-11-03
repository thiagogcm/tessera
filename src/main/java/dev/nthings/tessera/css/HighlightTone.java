package dev.nthings.tessera.css;

/**
 * Semantic highlight color tones backed by {@code color-mix(in oklch, var(--token) 24%,
 * transparent)} so highlights adapt automatically to {@code light-dark()} themes.
 */
public enum HighlightTone {
  ACCENT(Tokens.ACCENT_PRIMARY),
  WARNING(Tokens.STATUS_WARNING),
  SUCCESS(Tokens.STATUS_SUCCESS),
  DANGER(Tokens.STATUS_DANGER),
  INFO(Tokens.STATUS_INFO);

  private final DesignToken token;

  HighlightTone(DesignToken token) {
    this.token = token;
  }

  public DesignToken token() {
    return token;
  }

  public String toCssBackground() {
    return "color-mix(in oklch, " + token.varReference() + " 24%, transparent)";
  }
}
