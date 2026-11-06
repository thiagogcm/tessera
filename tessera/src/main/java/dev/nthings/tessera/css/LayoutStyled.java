package dev.nthings.tessera.css;

/// Display, positioning, flex and grid layout, sizing, overflow, and containers.
///
/// @param <SELF> concrete element or style builder type
public interface LayoutStyled<SELF extends LayoutStyled<SELF>> extends StyleTarget<SELF> {

  /// Applies a complete row or column. Later calls such as [#gap(CssLength)],
  /// [#blockSize(CssLength)], or [#flexShrink(int)] replace the matching declaration on this
  /// element.
  default SELF layout(FlexLayout layout) {
    layout.apply(style());
    return self();
  }

  default SELF flex() {
    style().put(CssProperty.DISPLAY, FlexLayout.Display.FLEX);
    return self();
  }

  default SELF inlineFlex() {
    style().put(CssProperty.DISPLAY, FlexLayout.Display.INLINE_FLEX);
    return self();
  }

  default SELF grid() {
    style().put(CssProperty.DISPLAY, "grid");
    return self();
  }

  default SELF block() {
    style().put(CssProperty.DISPLAY, "block");
    return self();
  }

  default SELF inlineBlock() {
    style().put(CssProperty.DISPLAY, "inline-block");
    return self();
  }

  default SELF hidden() {
    style().put(CssProperty.DISPLAY, "none");
    return self();
  }

  default SELF relative() {
    style().put(CssProperty.POSITION, "relative");
    return self();
  }

  default SELF absolute() {
    style().put(CssProperty.POSITION, "absolute");
    return self();
  }

  default SELF fixed() {
    style().put(CssProperty.POSITION, "fixed");
    return self();
  }

  default SELF sticky() {
    style().put(CssProperty.POSITION, "sticky");
    return self();
  }

  /// All four physical insets.
  default SELF inset(CssLength length) {
    style().put(CssProperty.INSET, length);
    return self();
  }

  default SELF insetBlockStart(CssLength length) {
    style().put(CssProperty.INSET_BLOCK_START, length);
    return self();
  }

  default SELF insetBlockEnd(CssLength length) {
    style().put(CssProperty.INSET_BLOCK_END, length);
    return self();
  }

  default SELF insetInlineStart(CssLength length) {
    style().put(CssProperty.INSET_INLINE_START, length);
    return self();
  }

  default SELF insetInlineEnd(CssLength length) {
    style().put(CssProperty.INSET_INLINE_END, length);
    return self();
  }

  default SELF zIndex(int level) {
    style().put(CssProperty.Z_INDEX, Integer.toString(level));
    return self();
  }

  default SELF overflowAuto() {
    style().put(CssProperty.OVERFLOW, "auto");
    return self();
  }

  default SELF overflowClip() {
    style().put(CssProperty.OVERFLOW, "clip");
    return self();
  }

  default SELF overflowHidden() {
    style().put(CssProperty.OVERFLOW, "hidden");
    return self();
  }

  default SELF overflowVisible() {
    style().put(CssProperty.OVERFLOW, "visible");
    return self();
  }

  default SELF overflowXAuto() {
    style().put(CssProperty.OVERFLOW_X, "auto");
    return self();
  }

  default SELF overflowYAuto() {
    style().put(CssProperty.OVERFLOW_Y, "auto");
    return self();
  }

  default SELF listStyleNone() {
    style().put(CssProperty.LIST_STYLE, "none");
    return self();
  }

  default SELF counterReset(CssIdent counter) {
    style().put(CssProperty.COUNTER_RESET, counter);
    return self();
  }

  default SELF counterIncrement(CssIdent counter) {
    style().put(CssProperty.COUNTER_INCREMENT, counter);
    return self();
  }

  /// Generated content of a `::before` or `::after` pseudo-element.
  default SELF content(CssContent content) {
    style().put(CssProperty.CONTENT, content);
    return self();
  }

  default SELF flexRow() {
    style().put(CssProperty.FLEX_DIRECTION, FlexLayout.Direction.ROW);
    return self();
  }

  default SELF flexCol() {
    style().put(CssProperty.FLEX_DIRECTION, FlexLayout.Direction.COLUMN);
    return self();
  }

  default SELF flexWrap() {
    style().put(CssProperty.FLEX_WRAP, FlexLayout.Wrap.WRAP);
    return self();
  }

  default SELF itemsStart() {
    style().put(CssProperty.ALIGN_ITEMS, FlexLayout.Align.START);
    return self();
  }

  default SELF itemsCenter() {
    style().put(CssProperty.ALIGN_ITEMS, FlexLayout.Align.CENTER);
    return self();
  }

  default SELF itemsEnd() {
    style().put(CssProperty.ALIGN_ITEMS, FlexLayout.Align.END);
    return self();
  }

  default SELF itemsStretch() {
    style().put(CssProperty.ALIGN_ITEMS, FlexLayout.Align.STRETCH);
    return self();
  }

  default SELF itemsBaseline() {
    style().put(CssProperty.ALIGN_ITEMS, FlexLayout.Align.BASELINE);
    return self();
  }

  default SELF justifyStart() {
    style().put(CssProperty.JUSTIFY_CONTENT, "flex-start");
    return self();
  }

  default SELF justifyCenter() {
    style().put(CssProperty.JUSTIFY_CONTENT, "center");
    return self();
  }

  default SELF justifyEnd() {
    style().put(CssProperty.JUSTIFY_CONTENT, "flex-end");
    return self();
  }

  default SELF justifyBetween() {
    style().put(CssProperty.JUSTIFY_CONTENT, "space-between");
    return self();
  }

  default SELF flexGrow(int factor) {
    style().put(CssProperty.FLEX_GROW, Integer.toString(factor));
    return self();
  }

  default SELF flexShrink(int factor) {
    style().put(CssProperty.FLEX_SHRINK, Integer.toString(factor));
    return self();
  }

  default SELF gridColumns(TrackList tracks) {
    style().put(CssProperty.GRID_TEMPLATE_COLUMNS, tracks);
    return self();
  }

  default SELF gridRows(TrackList tracks) {
    style().put(CssProperty.GRID_TEMPLATE_ROWS, tracks);
    return self();
  }

  /// A grid container whose rows are the parent grid's, spanning `trackSpan` of its rows.
  default SELF subgridRows(int trackSpan) {
    if (trackSpan < 1) {
      throw new IllegalArgumentException("trackSpan must be >= 1");
    }
    return grid().gridRow(GridPlacement.span(trackSpan)).gridRows(TrackList.subgrid());
  }

  default SELF gridColumn(GridPlacement placement) {
    style().put(CssProperty.GRID_COLUMN, placement);
    return self();
  }

  default SELF gridRow(GridPlacement placement) {
    style().put(CssProperty.GRID_ROW, placement);
    return self();
  }

  default SELF colSpan(int columns) {
    return gridColumn(GridPlacement.span(columns));
  }

  default SELF rowSpan(int rows) {
    return gridRow(GridPlacement.span(rows));
  }

  /// A grid with as many columns as fit, each at least `minColumnWidth` wide.
  default SELF gridAutoFit(CssLength minColumnWidth) {
    return grid().gridColumns(TrackList.autoFit(minColumnWidth));
  }

  default SELF gap(CssLength length) {
    style().put(CssProperty.GAP, length);
    return self();
  }

  default SELF rowGap(CssLength length) {
    style().put(CssProperty.ROW_GAP, length);
    return self();
  }

  default SELF colGap(CssLength length) {
    style().put(CssProperty.COLUMN_GAP, length);
    return self();
  }

  default SELF inlineSize(CssLength length) {
    style().put(CssProperty.INLINE_SIZE, length);
    return self();
  }

  default SELF blockSize(CssLength length) {
    style().put(CssProperty.BLOCK_SIZE, length);
    return self();
  }

  default SELF minInlineSize(CssLength length) {
    style().put(CssProperty.MIN_INLINE_SIZE, length);
    return self();
  }

  default SELF maxInlineSize(CssLength length) {
    style().put(CssProperty.MAX_INLINE_SIZE, length);
    return self();
  }

  default SELF minBlockSize(CssLength length) {
    style().put(CssProperty.MIN_BLOCK_SIZE, length);
    return self();
  }

  default SELF maxBlockSize(CssLength length) {
    style().put(CssProperty.MAX_BLOCK_SIZE, length);
    return self();
  }

  default SELF aspectRatio(AspectRatio ratio) {
    style().put(CssProperty.ASPECT_RATIO, ratio);
    return self();
  }

  default SELF objectCover() {
    style().put(CssProperty.OBJECT_FIT, "cover");
    return self();
  }

  default SELF fullWidth() {
    return inlineSize(CssLength.pct(100));
  }

  /// Unnamed `container-type: inline-size`.
  default SELF containerInlineSize() {
    style().put(CssProperty.CONTAINER_TYPE, "inline-size");
    return self();
  }

  /// Named inline-size query container (`container: <name> / inline-size`).
  default SELF containerInlineSize(CssIdent name) {
    style().put(CssProperty.CONTAINER_NAME, name);
    return containerInlineSize();
  }

  /// Resets `container-type` so a descendant does not query this element.
  default SELF containerNormal() {
    style().put(CssProperty.CONTAINER_TYPE, "normal");
    return self();
  }

  /// Anonymous `container-type: scroll-state` (queried by the nearest `scroll-state()` query).
  default SELF containerScrollState() {
    style().put(CssProperty.CONTAINER_TYPE, "scroll-state");
    return self();
  }

  default SELF containerScrollState(CssIdent name) {
    style().put(CssProperty.CONTAINER_NAME, name);
    return containerScrollState();
  }
}
