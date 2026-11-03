package dev.nthings.tessera.css;

/** CSS Text Decoration Level 4 line styles for underlines and strikethroughs. */
public enum UnderlineStyle {
  SOLID("solid"),
  WAVY("wavy"),
  DASHED("dashed"),
  DOTTED("dotted"),
  DOUBLE("double");

  private final String css;

  UnderlineStyle(String css) {
    this.css = css;
  }

  public String css() {
    return css;
  }
}
