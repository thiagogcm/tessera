package dev.nthings.tessera.css;

import java.util.Locale;

/// Scroll or view timeline tracking axis (`block` or `inline`).
public enum TimelineAxis {
  BLOCK,
  INLINE;

  private final String css = name().toLowerCase(Locale.ROOT).replace('_', '-');

  public String css() {
    return css;
  }
}
