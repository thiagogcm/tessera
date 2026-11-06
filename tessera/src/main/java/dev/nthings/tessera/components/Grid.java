package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssIdent;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.GridTrack;
import dev.nthings.tessera.css.TrackList;
import java.util.List;

/// Responsive CSS Grid component supporting intrinsic `auto-fit` columns, container-adaptive
/// multi-column tracks, and bento cell spanning.
public record Grid(TrackList columns, CssLength gap, boolean collapseOnNarrow, List<Element> items)
    implements RenderOnce {

  private static final CssIdent GRID_CONTAINER = CssIdent.of("tessera-grid");
  private static final CssLength MULTI_COL_MIN_WIDTH = CssLength.rem(36);

  public static Grid autoFit(CssLength minColumnWidth, CssLength gap, IntoElement... items) {
    return autoFit(minColumnWidth, gap, List.of(items));
  }

  public static Grid autoFit(
      CssLength minColumnWidth, CssLength gap, List<? extends IntoElement> items) {
    return new Grid(
        TrackList.autoFit(minColumnWidth),
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
    var template = TrackList.of(GridTrack.repeat(count, GridTrack.flexible()));
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
          .containerInlineSize(GRID_CONTAINER)
          .child(Elements.div().grid().gridColumns(columns).gap(gap).children(items));
    }
    return Elements.div()
        .containerInlineSize(GRID_CONTAINER)
        .child(
            Elements.div()
                .grid()
                .gridColumns(TrackList.of(GridTrack.flexible()))
                .gap(gap)
                .onContainer(GRID_CONTAINER, MULTI_COL_MIN_WIDTH, s -> s.gridColumns(columns))
                .children(items));
  }
}
