package dev.nthings.tessera.css;

/// Display, positioning, flex and grid layout, sizing, overflow, and containers. Part of [Styled].
///
/// @param <SELF> concrete element or style builder type
interface LayoutStyled<SELF extends LayoutStyled<SELF>> extends StyleTarget<SELF> {

  /// Sets `display`, `flex-direction`, `flex-wrap`, `align-items` and `gap` from `layout`. A later
  /// call that sets one of those (e.g. [#gap(CssLength)]) replaces only that declaration.
  default SELF layout(FlexLayout layout) {
    layout.apply(style());
    return self();
  }

  /// `display: flex`.
  default SELF flex() {
    style().put(CssProperty.DISPLAY, FlexLayout.Display.FLEX);
    return self();
  }

  /// `display: inline-flex`.
  default SELF inlineFlex() {
    style().put(CssProperty.DISPLAY, FlexLayout.Display.INLINE_FLEX);
    return self();
  }

  /// `display: grid`.
  default SELF grid() {
    style().put(CssProperty.DISPLAY, "grid");
    return self();
  }

  /// `display: block`.
  default SELF block() {
    style().put(CssProperty.DISPLAY, "block");
    return self();
  }

  /// `display: inline-block`.
  default SELF inlineBlock() {
    style().put(CssProperty.DISPLAY, "inline-block");
    return self();
  }

  /// Sets `display: none`, removing the element from layout and the accessibility tree.
  default SELF hidden() {
    style().put(CssProperty.DISPLAY, "none");
    return self();
  }

  /// `position: relative`.
  default SELF relative() {
    style().put(CssProperty.POSITION, "relative");
    return self();
  }

  /// `position: absolute`.
  default SELF absolute() {
    style().put(CssProperty.POSITION, "absolute");
    return self();
  }

  /// `position: fixed`.
  default SELF fixed() {
    style().put(CssProperty.POSITION, "fixed");
    return self();
  }

  /// `position: sticky`.
  default SELF sticky() {
    style().put(CssProperty.POSITION, "sticky");
    return self();
  }

  /// `inset`: all four insets. Removes the per-side insets.
  default SELF inset(CssLength length) {
    style().put(CssProperty.INSET, length);
    return self();
  }

  /// `inset-block-start`.
  default SELF insetBlockStart(CssLength length) {
    style().put(CssProperty.INSET_BLOCK_START, length);
    return self();
  }

  /// `inset-block-end`.
  default SELF insetBlockEnd(CssLength length) {
    style().put(CssProperty.INSET_BLOCK_END, length);
    return self();
  }

  /// `inset-inline-start`.
  default SELF insetInlineStart(CssLength length) {
    style().put(CssProperty.INSET_INLINE_START, length);
    return self();
  }

  /// `inset-inline-end`.
  default SELF insetInlineEnd(CssLength length) {
    style().put(CssProperty.INSET_INLINE_END, length);
    return self();
  }

  /// `z-index`.
  default SELF zIndex(int level) {
    style().put(CssProperty.Z_INDEX, Integer.toString(level));
    return self();
  }

  /// `overflow` on both axes. Removes [#overflowX(Overflow)] and [#overflowY(Overflow)].
  default SELF overflow(Overflow overflow) {
    style().put(CssProperty.OVERFLOW, overflow);
    return self();
  }

  /// `overflow-x`.
  default SELF overflowX(Overflow overflow) {
    style().put(CssProperty.OVERFLOW_X, overflow);
    return self();
  }

  /// `overflow-y`.
  default SELF overflowY(Overflow overflow) {
    style().put(CssProperty.OVERFLOW_Y, overflow);
    return self();
  }

  /// `list-style: none`, removing list markers.
  default SELF listStyleNone() {
    style().put(CssProperty.LIST_STYLE, "none");
    return self();
  }

  /// `counter-reset: <counter>`, starting the counter at 0 on this element.
  default SELF counterReset(Counter counter) {
    style().put(CssProperty.COUNTER_RESET, counter);
    return self();
  }

  /// `counter-increment: <counter>`, adding 1 to the counter on this element.
  default SELF counterIncrement(Counter counter) {
    style().put(CssProperty.COUNTER_INCREMENT, counter);
    return self();
  }

  /// Generated content of a `::before`, `::after` or `::scroll-button()` pseudo-element.
  default SELF content(CssContent content) {
    style().put(CssProperty.CONTENT, content);
    return self();
  }

  /// `flex-direction: row`.
  default SELF flexRow() {
    style().put(CssProperty.FLEX_DIRECTION, FlexLayout.Direction.ROW);
    return self();
  }

  /// `flex-direction: column`.
  default SELF flexCol() {
    style().put(CssProperty.FLEX_DIRECTION, FlexLayout.Direction.COLUMN);
    return self();
  }

  /// `flex-wrap: wrap`.
  default SELF flexWrap() {
    style().put(CssProperty.FLEX_WRAP, FlexLayout.Wrap.WRAP);
    return self();
  }

  /// `align-items: flex-start`.
  default SELF itemsStart() {
    style().put(CssProperty.ALIGN_ITEMS, FlexLayout.Align.START);
    return self();
  }

  /// `align-items: center`.
  default SELF itemsCenter() {
    style().put(CssProperty.ALIGN_ITEMS, FlexLayout.Align.CENTER);
    return self();
  }

  /// `align-items: flex-end`.
  default SELF itemsEnd() {
    style().put(CssProperty.ALIGN_ITEMS, FlexLayout.Align.END);
    return self();
  }

  /// `align-items: stretch`.
  default SELF itemsStretch() {
    style().put(CssProperty.ALIGN_ITEMS, FlexLayout.Align.STRETCH);
    return self();
  }

  /// `align-items: baseline`.
  default SELF itemsBaseline() {
    style().put(CssProperty.ALIGN_ITEMS, FlexLayout.Align.BASELINE);
    return self();
  }

  /// `justify-content: flex-start`.
  default SELF justifyStart() {
    style().put(CssProperty.JUSTIFY_CONTENT, "flex-start");
    return self();
  }

  /// `justify-content: center`.
  default SELF justifyCenter() {
    style().put(CssProperty.JUSTIFY_CONTENT, "center");
    return self();
  }

  /// `justify-content: flex-end`.
  default SELF justifyEnd() {
    style().put(CssProperty.JUSTIFY_CONTENT, "flex-end");
    return self();
  }

  /// `justify-content: space-between`.
  default SELF justifyBetween() {
    style().put(CssProperty.JUSTIFY_CONTENT, "space-between");
    return self();
  }

  /// `flex-grow`.
  default SELF flexGrow(int factor) {
    style().put(CssProperty.FLEX_GROW, Integer.toString(factor));
    return self();
  }

  /// `flex-shrink`.
  default SELF flexShrink(int factor) {
    style().put(CssProperty.FLEX_SHRINK, Integer.toString(factor));
    return self();
  }

  /// `grid-template-columns`.
  default SELF gridColumns(TrackList tracks) {
    style().put(CssProperty.GRID_TEMPLATE_COLUMNS, tracks);
    return self();
  }

  /// `grid-template-rows`.
  default SELF gridRows(TrackList tracks) {
    style().put(CssProperty.GRID_TEMPLATE_ROWS, tracks);
    return self();
  }

  /// Sets `display: grid` with rows inherited from the parent grid (`subgrid`), spanning
  /// `trackSpan` of the parent's rows. Throws IllegalArgumentException if `trackSpan` is below 1.
  default SELF subgridRows(int trackSpan) {
    return grid().gridRow(GridPlacement.span(trackSpan)).gridRows(TrackList.subgrid());
  }

  /// `grid-column`: where this item sits among its grid's columns.
  default SELF gridColumn(GridPlacement placement) {
    style().put(CssProperty.GRID_COLUMN, placement);
    return self();
  }

  /// `grid-row`: where this item sits among its grid's rows.
  default SELF gridRow(GridPlacement placement) {
    style().put(CssProperty.GRID_ROW, placement);
    return self();
  }

  /// Spans `columns` grid columns. Throws IllegalArgumentException if `columns` is below 1.
  default SELF colSpan(int columns) {
    return gridColumn(GridPlacement.span(columns));
  }

  /// Spans `rows` grid rows. Throws IllegalArgumentException if `rows` is below 1.
  default SELF rowSpan(int rows) {
    return gridRow(GridPlacement.span(rows));
  }

  /// A grid with as many columns as fit, each at least `minColumnWidth` wide.
  default SELF gridAutoFit(CssLength minColumnWidth) {
    return grid().gridColumns(TrackList.autoFit(minColumnWidth));
  }

  /// `gap` between rows and columns. Removes [#rowGap(CssLength)] and [#colGap(CssLength)].
  default SELF gap(CssLength length) {
    style().put(CssProperty.GAP, length);
    return self();
  }

  /// `row-gap`.
  default SELF rowGap(CssLength length) {
    style().put(CssProperty.ROW_GAP, length);
    return self();
  }

  /// `column-gap`.
  default SELF colGap(CssLength length) {
    style().put(CssProperty.COLUMN_GAP, length);
    return self();
  }

  /// `inline-size`, the logical width.
  default SELF inlineSize(CssLength length) {
    style().put(CssProperty.INLINE_SIZE, length);
    return self();
  }

  /// `block-size`, the logical height.
  default SELF blockSize(CssLength length) {
    style().put(CssProperty.BLOCK_SIZE, length);
    return self();
  }

  /// `min-inline-size`.
  default SELF minInlineSize(CssLength length) {
    style().put(CssProperty.MIN_INLINE_SIZE, length);
    return self();
  }

  /// `max-inline-size`.
  default SELF maxInlineSize(CssLength length) {
    style().put(CssProperty.MAX_INLINE_SIZE, length);
    return self();
  }

  /// `min-block-size`.
  default SELF minBlockSize(CssLength length) {
    style().put(CssProperty.MIN_BLOCK_SIZE, length);
    return self();
  }

  /// `max-block-size`.
  default SELF maxBlockSize(CssLength length) {
    style().put(CssProperty.MAX_BLOCK_SIZE, length);
    return self();
  }

  /// `aspect-ratio`. On an `<img>`, it also satisfies the `image-dimensions` check in place of
  /// `width` and `height` attributes.
  default SELF aspectRatio(AspectRatio ratio) {
    style().put(CssProperty.ASPECT_RATIO, ratio);
    return self();
  }

  /// `object-fit`, for images and other replaced content.
  default SELF objectFit(ObjectFit fit) {
    style().put(CssProperty.OBJECT_FIT, fit);
    return self();
  }

  /// Makes this element `container`: sets `container-type` to its types and, for a named
  /// container, `container-name`. Descendants then match the container's queries, such as
  /// `on(container.atLeast(rem(36)), ...)`. Containment changes layout (an inline-size container
  /// does not size to its content), so declare it only where queries need it.
  default SELF container(Container container) {
    var name = container.ident();
    if (name != null) {
      style().put(CssProperty.CONTAINER_NAME, name);
    }
    style().put(CssProperty.CONTAINER_TYPE, container.typeList());
    return self();
  }

  /// `container-type: normal`: this element stops being a size or scroll-state container, so
  /// descendants' queries look past it. Use it to undo a container a component declares.
  default SELF containerNone() {
    style().put(CssProperty.CONTAINER_TYPE, "normal");
    return self();
  }
}
