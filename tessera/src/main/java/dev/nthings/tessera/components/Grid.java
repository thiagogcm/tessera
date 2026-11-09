package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.ParentElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.Container;
import dev.nthings.tessera.css.ContainerQuery;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.GridTrack;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.TrackList;
import java.util.ArrayList;
import java.util.List;

/// Grid of items in columns. Create one with [#autoFit] (as many columns as fit, each at least
/// the minimum width) or [#columns] (a fixed count), and add more items as children. A `columns`
/// grid stacks into one column while it is narrower than 36rem. Wrap an item in [#cell] to span
/// columns or rows; spans apply only when the grid is 36rem or wider.
public final class Grid extends BuiltinComponent implements ParentElement<Grid> {

  private static final Container GRID_CONTAINER =
      Container.named("tessera-grid", Container.Type.INLINE_SIZE);
  private static final ContainerQuery MULTI_COLUMN = GRID_CONTAINER.atLeast(CssLength.rem(36));

  private final TrackList columns;
  private final boolean collapseOnNarrow;
  private CssLength gap = Tokens.SPACE_4;
  private final List<Element> items = new ArrayList<>();

  private Grid(TrackList columns, boolean collapseOnNarrow) {
    this.columns = columns;
    this.collapseOnNarrow = collapseOnNarrow;
  }

  /// Creates a grid with as many columns as fit, each at least `minColumnWidth` wide, holding
  /// `items`. A grid narrower than `minColumnWidth` gets one full-width column.
  public static Grid autoFit(CssLength minColumnWidth, IntoElement... items) {
    return new Grid(TrackList.autoFit(minColumnWidth), false).children(items);
  }

  /// Creates a grid of `count` equal columns holding `items`, which stacks into one column while
  /// the grid is narrower than 36rem. Throws IllegalArgumentException if `count` is less than 1.
  public static Grid columns(int count, IntoElement... items) {
    var template = TrackList.of(GridTrack.repeat(count, GridTrack.flexible()));
    return new Grid(template, count > 1).children(items);
  }

  /// Wraps `content` in a cell spanning `colSpan` columns and `rowSpan` rows. Spans apply only
  /// when the enclosing grid is 36rem or wider; otherwise the cell takes one column and row.
  public static TagElement cell(int colSpan, int rowSpan, IntoElement content) {
    return Elements.div().on(MULTI_COLUMN, s -> s.colSpan(colSpan).rowSpan(rowSpan)).child(content);
  }

  @Override
  public Grid self() {
    return this;
  }

  @Override
  public void appendChild(Element child) {
    items.add(child);
  }

  /// Sets the row and column gap. The default is [Tokens#SPACE_4].
  public Grid gap(CssLength gap) {
    this.gap = gap;
    return this;
  }

  @Override
  IntoElement render(RenderContext context) {
    var grid = Elements.div().grid();
    if (collapseOnNarrow) {
      grid.gridColumns(TrackList.of(GridTrack.flexible()))
          .gap(gap)
          .on(MULTI_COLUMN, s -> s.gridColumns(columns));
    } else {
      grid.gridColumns(columns).gap(gap);
    }
    return Elements.div().container(GRID_CONTAINER).child(grid.children(items));
  }
}
