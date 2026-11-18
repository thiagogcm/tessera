package dev.nthings.tessera.core;

import java.util.Locale;

/// SVG element names for structured inline graphics, created with [Elements#svg(SvgTag)]. Their
/// namespace is SVG: static output writes them inside `<svg>` and a browser root creates them with
/// `createElementNS`, so attribute names such as `viewBox` keep their case.
public enum SvgTag implements TagName {
  SVG,
  G,
  PATH,
  CIRCLE,
  ELLIPSE,
  LINE,
  POLYLINE,
  POLYGON,
  RECT;

  private final String tagName = name().toLowerCase(Locale.ROOT);

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
