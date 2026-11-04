package dev.nthings.tessera.css;

import java.util.Locale;

/// CSS Text Decoration Level 4 line styles for underlines and strikethroughs.
public enum UnderlineStyle {
  SOLID,
  WAVY,
  DASHED,
  DOTTED,
  DOUBLE;

  private final String css = name().toLowerCase(Locale.ROOT).replace('_', '-');

  public String css() {
    return css;
  }
}
