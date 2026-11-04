package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Styled;
import java.util.List;

/// Responsive CSS Grid component supporting intrinsic `auto-fit` columns, container-adaptive
/// multi-column tracks, and bento cell spanning.
public record Grid(
    String columnsTemplate, CssLength gap, boolean collapseOnNarrow, List<Element> items)
    implements RenderOnce {

  private static final String GRID_CONTAINER = "tessera-grid";
  private static final CssLength MULTI_COL_MIN_WIDTH = CssLength.rem(36);

  public Grid(String columnsTemplate, CssLength gap, List<Element> items) {
    this(columnsTemplate, gap, false, items);
  }

  public static Grid autoFit(CssLength minColumnWidth, CssLength gap, IntoElement... items) {
    return autoFit(minColumnWidth, gap, List.of(items));
  }

  public static Grid autoFit(
      CssLength minColumnWidth, CssLength gap, List<? extends IntoElement> items) {
    return new Grid(
        Styled.autoFitTracks(minColumnWidth),
        gap,
        false,
        items.stream().map(IntoElement::intoElement).toList());
  }

  public static Grid columns(int count, CssLength gap, IntoElement... items) {
    return columns(count, gap, List.of(items));
  }

  public static Grid columns(int count, CssLength gap, List<? extends IntoElement> items) {
    if (count <= 0) {
      throw new IllegalArgumentException("Grid column count must be > 0");
    }
    String template = "repeat(" + count + ", minmax(0, 1fr))";
    return new Grid(
        template, gap, count > 1, items.stream().map(IntoElement::intoElement).toList());
  }

  public static TagElement cell(int colSpan, int rowSpan, IntoElement content) {
    return Elements.div()
        .onContainer(GRID_CONTAINER, MULTI_COL_MIN_WIDTH, s -> s.colSpan(colSpan).rowSpan(rowSpan))
        .child(content);
  }

  @Override
  public Element render() {
    if (!collapseOnNarrow) {
      return Elements.div()
          .container(GRID_CONTAINER)
          .child(Elements.div().grid().gridColumns(columnsTemplate).gap(gap).children(items));
    }
    return Elements.div()
        .container(GRID_CONTAINER)
        .child(
            Elements.div()
                .grid()
                .gridColumns("minmax(0, 1fr)")
                .gap(gap)
                .onContainer(
                    GRID_CONTAINER, MULTI_COL_MIN_WIDTH, s -> s.gridColumns(columnsTemplate))
                .children(items));
  }
}
