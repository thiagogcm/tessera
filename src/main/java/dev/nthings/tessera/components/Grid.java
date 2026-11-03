package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import java.util.List;

/**
 * Responsive CSS Grid component supporting intrinsic {@code auto-fit} columns, fixed column counts,
 * and bento cell spanning.
 */
public record Grid(String columnsTemplate, CssLength gap, List<Element> items)
    implements RenderOnce {

  public static Grid autoFit(CssLength minColumnWidth, CssLength gap, IntoElement... items) {
    return autoFit(minColumnWidth, gap, List.of(items));
  }

  public static Grid autoFit(
      CssLength minColumnWidth, CssLength gap, List<? extends IntoElement> items) {
    String template = "repeat(auto-fit, minmax(min(100%, " + minColumnWidth.toCss() + "), 1fr))";
    return new Grid(template, gap, items.stream().map(IntoElement::intoElement).toList());
  }

  public static Grid columns(int count, CssLength gap, IntoElement... items) {
    return columns(count, gap, List.of(items));
  }

  public static Grid columns(int count, CssLength gap, List<? extends IntoElement> items) {
    if (count <= 0) {
      throw new IllegalArgumentException("Grid column count must be > 0");
    }
    String template = "repeat(" + count + ", minmax(0, 1fr))";
    return new Grid(template, gap, items.stream().map(IntoElement::intoElement).toList());
  }

  public static TagElement cell(int colSpan, int rowSpan, IntoElement content) {
    return Elements.div().colSpan(colSpan).rowSpan(rowSpan).child(content);
  }

  @Override
  public Element render() {
    return Elements.div().grid().gridColumns(columnsTemplate).gap(gap).children(items);
  }
}
