package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.svg;

import dev.nthings.tessera.core.SvgTag;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.internal.foundation.Decimals;

/// Line icons as structured SVG, in the style of Lucide and Feather: a square `viewBox`, no fill,
/// round caps and joins, stroked in `currentColor` so the icon takes the text color around it.
///
/// ```java
/// static TagElement rss(int size) {
///   return Icon.stroked(
///       size,
///       Icon.path("M4 11a9 9 0 0 1 9 9"),
///       Icon.path("M4 4a16 16 0 0 1 16 16"),
///       Icon.circle(5, 19, 1));
/// }
/// ```
///
/// An icon is decorative and hidden from assistive technology. When it carries meaning on its own,
/// name it: `.ariaHidden(false).role(Role.IMG).ariaLabel("RSS feed")`.
public final class Icon {

  private Icon() {}

  /// A `size`-pixel icon drawn on a 24-unit grid with a stroke 2 units wide.
  public static TagElement stroked(int size, TagElement... shapes) {
    return stroked(size, 24, 2, shapes);
  }

  /// A `size`-pixel icon drawn on a `box`-unit square grid with a stroke `strokeWidth` units wide.
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

  /// A `<path>` with path data `d`.
  public static TagElement path(String d) {
    return svg(SvgTag.PATH).attr("d", d);
  }

  public static TagElement circle(double cx, double cy, double r) {
    return svg(SvgTag.CIRCLE)
        .attr("cx", Decimals.format(cx))
        .attr("cy", Decimals.format(cy))
        .attr("r", Decimals.format(r));
  }

  public static TagElement ellipse(double cx, double cy, double rx, double ry) {
    return svg(SvgTag.ELLIPSE)
        .attr("cx", Decimals.format(cx))
        .attr("cy", Decimals.format(cy))
        .attr("rx", Decimals.format(rx))
        .attr("ry", Decimals.format(ry));
  }

  public static TagElement line(double x1, double y1, double x2, double y2) {
    return svg(SvgTag.LINE)
        .attr("x1", Decimals.format(x1))
        .attr("y1", Decimals.format(y1))
        .attr("x2", Decimals.format(x2))
        .attr("y2", Decimals.format(y2));
  }

  /// A rectangle with corners rounded by `radius` (0 for square corners).
  public static TagElement rect(double x, double y, double width, double height, double radius) {
    var rect =
        svg(SvgTag.RECT)
            .attr("x", Decimals.format(x))
            .attr("y", Decimals.format(y))
            .attr("width", Decimals.format(width))
            .attr("height", Decimals.format(height));
    return radius == 0 ? rect : rect.attr("rx", Decimals.format(radius));
  }

  /// A `<polyline>` through `points`, such as `"9 18 15 12 9 6"`.
  public static TagElement polyline(String points) {
    return svg(SvgTag.POLYLINE).attr("points", points);
  }

  /// A closed `<polygon>` through `points`.
  public static TagElement polygon(String points) {
    return svg(SvgTag.POLYGON).attr("points", points);
  }
}
