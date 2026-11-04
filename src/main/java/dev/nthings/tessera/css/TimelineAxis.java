package dev.nthings.tessera.css;

/// Scroll or view timeline tracking axis (`block` or `inline`).
public enum TimelineAxis {
  BLOCK("block"),
  INLINE("inline");

  private final String css;

  TimelineAxis(String css) {
    this.css = css;
  }

  public String css() {
    return css;
  }
}
