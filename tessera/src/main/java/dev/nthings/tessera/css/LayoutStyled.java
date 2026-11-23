package dev.nthings.tessera.css;

/// Display, positioning, flex and grid layout, sizing, overflow, and containers. Part of [Styled].
///
/// @param <SELF> concrete element or style builder type
interface LayoutStyled<SELF extends LayoutStyled<SELF>> extends StyleTarget<SELF> {

  /// Sets `display`, `flex-direction`, `flex-wrap`, `align-items` and `gap` from `layout`. A later
  /// call that sets one of those (e.g. [#gap(CssLength)]) replaces only that declaration.
  default SELF layout(FlexLayout layout) {
    layout.writeTo(StyleTarget.declarations(this));
    return self();
  }

  /// `display: flex`.
  default SELF flex() {
    StyleTarget.declarations(this).put(CssProperty.DISPLAY, FlexLayout.Display.FLEX);
    return self();
  }

  /// `display: inline-flex`.
  default SELF inlineFlex() {
    StyleTarget.declarations(this).put(CssProperty.DISPLAY, FlexLayout.Display.INLINE_FLEX);
    return self();
  }

  /// `display: grid`.
  default SELF grid() {
    StyleTarget.declarations(this).put(CssProperty.DISPLAY, "grid");
    return self();
  }

  /// `display: block`.
  default SELF block() {
    StyleTarget.declarations(this).put(CssProperty.DISPLAY, "block");
    return self();
  }

  /// `display: inline-block`.
  default SELF inlineBlock() {
    StyleTarget.declarations(this).put(CssProperty.DISPLAY, "inline-block");
    return self();
  }

  /// Sets `display: none`, removing the element from layout and the accessibility tree.
  default SELF hidden() {
    StyleTarget.declarations(this).put(CssProperty.DISPLAY, "none");
    return self();
  }

  /// `position: relative`.
  default SELF relative() {
    StyleTarget.declarations(this).put(CssProperty.POSITION, "relative");
    return self();
  }

  /// `position: absolute`.
  default SELF absolute() {
    StyleTarget.declarations(this).put(CssProperty.POSITION, "absolute");
    return self();
  }

  /// `position: fixed`.
  default SELF fixed() {
    StyleTarget.declarations(this).put(CssProperty.POSITION, "fixed");
    return self();
  }

  /// `position: sticky`.
  default SELF sticky() {
    StyleTarget.declarations(this).put(CssProperty.POSITION, "sticky");
    return self();
  }

  /// `inset`: all four insets. Removes the per-side insets.
  default SELF inset(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.INSET, length);
    return self();
  }

  /// `inset-block-start`.
  default SELF insetBlockStart(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.INSET_BLOCK_START, length);
    return self();
  }

  /// `inset-block-end`.
  default SELF insetBlockEnd(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.INSET_BLOCK_END, length);
    return self();
  }

  /// `inset-inline-start`.
  default SELF insetInlineStart(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.INSET_INLINE_START, length);
    return self();
  }

  /// `inset-inline-end`.
  default SELF insetInlineEnd(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.INSET_INLINE_END, length);
    return self();
  }

  /// `z-index`.
  default SELF zIndex(int level) {
    StyleTarget.declarations(this).put(CssProperty.Z_INDEX, Integer.toString(level));
    return self();
  }

  /// `overflow` on both axes. Removes [#overflowX(Overflow)] and [#overflowY(Overflow)].
  default SELF overflow(Overflow overflow) {
    StyleTarget.declarations(this).put(CssProperty.OVERFLOW, overflow);
    return self();
  }

  /// `overflow-x`.
  default SELF overflowX(Overflow overflow) {
    StyleTarget.declarations(this).put(CssProperty.OVERFLOW_X, overflow);
    return self();
  }

  /// `overflow-y`.
  default SELF overflowY(Overflow overflow) {
    StyleTarget.declarations(this).put(CssProperty.OVERFLOW_Y, overflow);
    return self();
  }

  /// `list-style: none`, removing list markers.
  default SELF listStyleNone() {
    StyleTarget.declarations(this).put(CssProperty.LIST_STYLE, "none");
    return self();
  }

  /// `counter-reset: <counter>`, starting the counter at 0 on this element.
  default SELF counterReset(Counter counter) {
    StyleTarget.declarations(this).put(CssProperty.COUNTER_RESET, counter);
    return self();
  }

  /// `counter-increment: <counter>`, adding 1 to the counter on this element.
  default SELF counterIncrement(Counter counter) {
    StyleTarget.declarations(this).put(CssProperty.COUNTER_INCREMENT, counter);
    return self();
  }

  /// Generated content of a `::before`, `::after` or `::scroll-button()` pseudo-element.
  default SELF content(CssContent content) {
    StyleTarget.declarations(this).put(CssProperty.CONTENT, content);
    return self();
  }

  /// `flex-direction: row`.
  default SELF flexRow() {
    StyleTarget.declarations(this).put(CssProperty.FLEX_DIRECTION, FlexLayout.Direction.ROW);
    return self();
  }

  /// `flex-direction: column`.
  default SELF flexCol() {
    StyleTarget.declarations(this).put(CssProperty.FLEX_DIRECTION, FlexLayout.Direction.COLUMN);
    return self();
  }

  /// `flex-wrap: wrap`.
  default SELF flexWrap() {
    StyleTarget.declarations(this).put(CssProperty.FLEX_WRAP, FlexLayout.Wrap.WRAP);
    return self();
  }

  /// `align-items: flex-start`.
  default SELF itemsStart() {
    StyleTarget.declarations(this).put(CssProperty.ALIGN_ITEMS, FlexLayout.Align.START);
    return self();
  }

  /// `align-items: center`.
  default SELF itemsCenter() {
    StyleTarget.declarations(this).put(CssProperty.ALIGN_ITEMS, FlexLayout.Align.CENTER);
    return self();
  }

  /// `align-items: flex-end`.
  default SELF itemsEnd() {
    StyleTarget.declarations(this).put(CssProperty.ALIGN_ITEMS, FlexLayout.Align.END);
    return self();
  }

  /// `align-items: stretch`.
  default SELF itemsStretch() {
    StyleTarget.declarations(this).put(CssProperty.ALIGN_ITEMS, FlexLayout.Align.STRETCH);
    return self();
  }

  /// `align-items: baseline`.
  default SELF itemsBaseline() {
    StyleTarget.declarations(this).put(CssProperty.ALIGN_ITEMS, FlexLayout.Align.BASELINE);
    return self();
  }

  /// `justify-content: flex-start`.
  default SELF justifyStart() {
    StyleTarget.declarations(this).put(CssProperty.JUSTIFY_CONTENT, "flex-start");
    return self();
  }

  /// `justify-content: center`.
  default SELF justifyCenter() {
    StyleTarget.declarations(this).put(CssProperty.JUSTIFY_CONTENT, "center");
    return self();
  }

  /// `justify-content: flex-end`.
  default SELF justifyEnd() {
    StyleTarget.declarations(this).put(CssProperty.JUSTIFY_CONTENT, "flex-end");
    return self();
  }

  /// `justify-content: space-between`.
  default SELF justifyBetween() {
    StyleTarget.declarations(this).put(CssProperty.JUSTIFY_CONTENT, "space-between");
    return self();
  }

  /// `flex-grow`.
  default SELF flexGrow(int factor) {
    StyleTarget.declarations(this).put(CssProperty.FLEX_GROW, Integer.toString(factor));
    return self();
  }

  /// `flex-shrink`.
  default SELF flexShrink(int factor) {
    StyleTarget.declarations(this).put(CssProperty.FLEX_SHRINK, Integer.toString(factor));
    return self();
  }

  /// `grid-template-columns`.
  default SELF gridColumns(TrackList tracks) {
    StyleTarget.declarations(this).put(CssProperty.GRID_TEMPLATE_COLUMNS, tracks);
    return self();
  }

  /// `grid-template-rows`.
  default SELF gridRows(TrackList tracks) {
    StyleTarget.declarations(this).put(CssProperty.GRID_TEMPLATE_ROWS, tracks);
    return self();
  }

  /// Sets `display: grid` with rows inherited from the parent grid (`subgrid`), spanning
  /// `trackSpan` of the parent's rows. Throws IllegalArgumentException if `trackSpan` is below 1.
  default SELF subgridRows(int trackSpan) {
    return grid().gridRow(GridPlacement.span(trackSpan)).gridRows(TrackList.subgrid());
  }

  /// `grid-column`: where this item sits among its grid's columns.
  default SELF gridColumn(GridPlacement placement) {
    StyleTarget.declarations(this).put(CssProperty.GRID_COLUMN, placement);
    return self();
  }

  /// `grid-row`: where this item sits among its grid's rows.
  default SELF gridRow(GridPlacement placement) {
    StyleTarget.declarations(this).put(CssProperty.GRID_ROW, placement);
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
    StyleTarget.declarations(this).put(CssProperty.GAP, length);
    return self();
  }

  /// `row-gap`.
  default SELF rowGap(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.ROW_GAP, length);
    return self();
  }

  /// `column-gap`.
  default SELF colGap(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.COLUMN_GAP, length);
    return self();
  }

  /// `inline-size`, the logical width.
  default SELF inlineSize(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.INLINE_SIZE, length);
    return self();
  }

  /// `block-size`, the logical height.
  default SELF blockSize(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.BLOCK_SIZE, length);
    return self();
  }

  /// `min-inline-size`.
  default SELF minInlineSize(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.MIN_INLINE_SIZE, length);
    return self();
  }

  /// `max-inline-size`.
  default SELF maxInlineSize(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.MAX_INLINE_SIZE, length);
    return self();
  }

  /// `min-block-size`.
  default SELF minBlockSize(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.MIN_BLOCK_SIZE, length);
    return self();
  }

  /// `max-block-size`.
  default SELF maxBlockSize(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.MAX_BLOCK_SIZE, length);
    return self();
  }

  /// `aspect-ratio`. On an `<img>`, it also satisfies the `image-dimensions` check in place of
  /// `width` and `height` attributes.
  default SELF aspectRatio(AspectRatio ratio) {
    StyleTarget.declarations(this).put(CssProperty.ASPECT_RATIO, ratio);
    return self();
  }

  /// Lets the browser skip laying out and painting this element's contents while they are far from
  /// the viewport (`content-visibility: auto`), so long documents, lists and tables render only
  /// what is near the screen. Until the element first renders it takes `intrinsicBlockSize` as its
  /// block size, and afterwards the size it last rendered at (`contain-intrinsic-block-size: auto
  /// <length>`), so the scrollbar stays steady; estimate the typical rendered size.
  ///
  /// Skipped contents stay in the accessibility tree, the tab order and find-in-page, and render as
  /// soon as they are focused, found or scrolled near. The element contains its layout, style and
  /// paint: it clips what overflows it, such as shadows, and becomes the containing block of
  /// positioned descendants. Its inline size comes from its container, so use it on block-level
  /// sections stacked in the block direction rather than on shrink-to-fit items.
  default SELF contentVisibilityAuto(CssLength intrinsicBlockSize) {
    StyleTarget.declarations(this)
        .put(CssProperty.CONTENT_VISIBILITY, "auto")
        .put(CssProperty.CONTAIN_INTRINSIC_BLOCK_SIZE, "auto " + intrinsicBlockSize.toCss());
    return self();
  }

  /// `object-fit`, for images and other replaced content.
  default SELF objectFit(ObjectFit fit) {
    StyleTarget.declarations(this).put(CssProperty.OBJECT_FIT, fit);
    return self();
  }

  /// Makes this element `container`: sets `container-type` to its types and, for a named container,
  /// `container-name`. Descendants then match the container's queries, such as
  /// `on(container.atLeast(rem(36)), ...)`. Containment changes layout (an inline-size container
  /// does not size to its content), so declare it only where queries need it.
  default SELF container(Container container) {
    var style = StyleTarget.declarations(this);
    var name = container.ident();
    if (name != null) {
      style.put(CssProperty.CONTAINER_NAME, name);
    }
    style.put(CssProperty.CONTAINER_TYPE, container.typeList());
    return self();
  }

  /// `container-type: normal`: this element stops being a size or scroll-state container, so
  /// descendants' queries look past it. Use it to undo a container a component declares.
  default SELF containerNone() {
    StyleTarget.declarations(this).put(CssProperty.CONTAINER_TYPE, "normal");
    return self();
  }
}
