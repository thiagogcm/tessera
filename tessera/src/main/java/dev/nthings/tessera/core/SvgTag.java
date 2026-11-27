package dev.nthings.tessera.core;

/// SVG element names for structured inline graphics, created with [Elements#el(TagName)]. Their
/// namespace is SVG: static output writes them inside `<svg>` and a browser root creates them with
/// `createElementNS`, so attribute names such as `viewBox` keep their case.
public enum SvgTag implements TagName {
  SVG("svg"),
  G("g"),
  PATH("path"),
  CIRCLE("circle"),
  ELLIPSE("ellipse"),
  LINE("line"),
  POLYLINE("polyline"),
  POLYGON("polygon"),
  RECT("rect"),
  TEXT("text"),
  TSPAN("tspan"),
  TITLE("title"),
  DESC("desc"),
  DEFS("defs"),
  SYMBOL("symbol"),
  USE("use"),
  LINEAR_GRADIENT("linearGradient"),
  RADIAL_GRADIENT("radialGradient"),
  STOP("stop"),
  CLIP_PATH("clipPath"),
  MASK("mask"),
  PATTERN("pattern"),
  MARKER("marker");

  private final String tagName;

  SvgTag(String tagName) {
    this.tagName = tagName;
  }

  @Override
  public String tagName() {
    return tagName;
  }

  /// SVG elements have an end tag in HTML output, so none is void.
  @Override
  public boolean isVoid() {
    return false;
  }

  @Override
  public Namespace namespace() {
    return Namespace.SVG;
  }
}
