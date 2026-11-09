package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.Container;
import dev.nthings.tessera.css.ContainerQuery;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.GridTrack;
import dev.nthings.tessera.css.TrackList;
import java.util.List;

/// Grid of items in columns. Create one with [#autoFit] (as many columns as fit, each at least
/// the minimum width) or [#columns] (a fixed count). A `columns` grid stacks into one column
/// while it is narrower than 36rem. Wrap an item in [#cell] to span columns or rows; spans apply
/// only when the grid is 36rem or wider.
public record Grid(TrackList columns, CssLength gap, boolean collapseOnNarrow, List<Element> items)
    implements Component {

  private static final Container GRID_CONTAINER =
      Container.named("tessera-grid", Container.Type.INLINE_SIZE);
  private static final ContainerQuery MULTI_COLUMN = GRID_CONTAINER.atLeast(CssLength.rem(36));

  /// Creates a grid with as many columns as fit, each at least `minColumnWidth` wide. A grid
  /// narrower than `minColumnWidth` gets one full-width column.
  public static Grid autoFit(CssLength minColumnWidth, CssLength gap, IntoElement... items) {
    return autoFit(minColumnWidth, gap, List.of(items));
  }

  /// Creates a grid with as many columns as fit, each at least `minColumnWidth` wide.
  public static Grid autoFit(
      CssLength minColumnWidth, CssLength gap, List<? extends IntoElement> items) {
    return new Grid(
        TrackList.autoFit(minColumnWidth),
        gap,
        false,
        items.stream().map(IntoElement::intoElement).toList());
  }

  /// Creates a grid of `count` equal columns that stack into one column while the grid is
  /// narrower than 36rem. Throws IllegalArgumentException if `count` is less than 1.
  public static Grid columns(int count, CssLength gap, IntoElement... items) {
    return columns(count, gap, List.of(items));
  }

  /// Creates a grid of `count` equal columns that stack into one column while the grid is
  /// narrower than 36rem. Throws IllegalArgumentException if `count` is less than 1.
  public static Grid columns(int count, CssLength gap, List<? extends IntoElement> items) {
    var template = TrackList.of(GridTrack.repeat(count, GridTrack.flexible()));
    return new Grid(
        template, gap, count > 1, items.stream().map(IntoElement::intoElement).toList());
  }

  /// Wraps `content` in a cell spanning `colSpan` columns and `rowSpan` rows. Spans apply only
  /// when the enclosing grid is 36rem or wider; otherwise the cell takes one column and row.
  public static TagElement cell(int colSpan, int rowSpan, IntoElement content) {
    return Elements.div().on(MULTI_COLUMN, s -> s.colSpan(colSpan).rowSpan(rowSpan)).child(content);
  }

  @Override
  public Element render(RenderContext context) {
    if (!collapseOnNarrow) {
      return Elements.div()
          .container(GRID_CONTAINER)
          .child(Elements.div().grid().gridColumns(columns).gap(gap).children(items));
    }
    return Elements.div()
        .container(GRID_CONTAINER)
        .child(
            Elements.div()
                .grid()
                .gridColumns(TrackList.of(GridTrack.flexible()))
                .gap(gap)
                .on(MULTI_COLUMN, s -> s.gridColumns(columns))
                .children(items));
  }
}
