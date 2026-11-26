package dev.nthings.tessera.internal.components;

import static dev.nthings.tessera.core.Elements.svg;

import dev.nthings.tessera.core.SvgTag;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.internal.foundation.Decimals;

/// The bundled line icons, built as structured SVG so they render the same in static output and in
/// a browser root. Each icon is decorative: hidden from assistive technology and drawn with
/// `currentColor`.
public final class Icons {

  private Icons() {}

  /// A `size`-pixel icon over a square `viewBox` of `box` units, stroked `strokeWidth` wide.
  public static TagElement stroked(int size, int box, double strokeWidth, TagElement... shapes) {
    return svg(SvgTag.SVG)
        .attr("width", Integer.toString(size))
        .attr("height", Integer.toString(size))
        .attr("viewBox", "0 0 " + box + " " + box)
        .attr("fill", "none")
        .attr("stroke", "currentColor")
        .attr("stroke-width", Decimals.format(strokeWidth))
        .attr("stroke-linecap", "round")
        .attr("stroke-linejoin", "round")
        .ariaHidden(true)
        .children(shapes);
  }

  public static TagElement circle(double cx, double cy, double r) {
    return svg(SvgTag.CIRCLE)
        .attr("cx", Decimals.format(cx))
        .attr("cy", Decimals.format(cy))
        .attr("r", Decimals.format(r));
  }

  public static TagElement line(double x1, double y1, double x2, double y2) {
    return svg(SvgTag.LINE)
        .attr("x1", Decimals.format(x1))
        .attr("y1", Decimals.format(y1))
        .attr("x2", Decimals.format(x2))
        .attr("y2", Decimals.format(y2));
  }

  public static TagElement path(String d) {
    return svg(SvgTag.PATH).attr("d", d);
  }

  public static TagElement polyline(String points) {
    return svg(SvgTag.POLYLINE).attr("points", points);
  }

  public static TagElement polygon(String points) {
    return svg(SvgTag.POLYGON).attr("points", points);
  }

  /// The magnifier of search fields and triggers.
  public static TagElement search() {
    return stroked(16, 24, 2, circle(11, 11, 8), line(21, 21, 16.65, 16.65));
  }

  /// The right-pointing chevron of disclosure rows.
  public static TagElement chevronRight(int size) {
    return stroked(size, 24, 2.2, polyline("9 18 15 12 9 6"));
  }

  /// The chain link of heading permalinks.
  public static TagElement link(int size) {
    return stroked(
        size,
        24,
        2,
        path("M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"),
        path("M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"));
  }

  /// The sun of the light color scheme.
  public static TagElement sun() {
    return stroked(
        18,
        24,
        2,
        circle(12, 12, 4),
        path(
            "M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2"
                + "M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"));
  }

  /// The crescent moon of the dark color scheme.
  public static TagElement moon() {
    return stroked(18, 24, 2, path("M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"));
  }

  /// The monitor of the system color scheme.
  public static TagElement monitor() {
    return stroked(
        18,
        24,
        2,
        path("M4 3h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z"),
        path("M8 21h8M12 17v4"));
  }
}
