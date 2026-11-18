package dev.nthings.tessera.css;

/// Scroll behavior, snapping, and scrollbars. Part of [Styled].
///
/// @param <SELF> concrete element or style builder type
interface ScrollStyled<SELF extends ScrollStyled<SELF>> extends StyleTarget<SELF> {

  /// `overflow-anchor: none`: content changes above the viewport no longer adjust the scroll
  /// position.
  default SELF overflowAnchorNone() {
    StyleTarget.declarations(this).put(CssProperty.OVERFLOW_ANCHOR, "none");
    return self();
  }

  /// `scroll-behavior: smooth` for fragment links and programmatic scrolls in this scroller.
  default SELF scrollBehaviorSmooth() {
    StyleTarget.declarations(this).put(CssProperty.SCROLL_BEHAVIOR, "smooth");
    return self();
  }

  /// `scroll-snap-type`, on the scroller.
  default SELF scrollSnapType(ScrollSnap snap) {
    StyleTarget.declarations(this).put(CssProperty.SCROLL_SNAP_TYPE, snap);
    return self();
  }

  /// `scroll-snap-align`, on a snap child.
  default SELF scrollSnapAlign(SnapAlign align) {
    StyleTarget.declarations(this).put(CssProperty.SCROLL_SNAP_ALIGN, align);
    return self();
  }

  /// `scroll-margin-block-start`: space kept above the element when it is scrolled into view, such
  /// as below a sticky header.
  default SELF scrollMarginBlockStart(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.SCROLL_MARGIN_BLOCK_START, length);
    return self();
  }

  /// `scroll-target-group: auto`: same-page links inside become scroll markers, so the one for the
  /// visible target matches `:target-current`.
  default SELF scrollTargetGroupAuto() {
    StyleTarget.declarations(this).put(CssProperty.SCROLL_TARGET_GROUP, "auto");
    return self();
  }

  /// `scroll-marker-group`: where the scroller's generated `::scroll-marker`s are placed.
  default SELF scrollMarkerGroup(ScrollMarkerGroup position) {
    StyleTarget.declarations(this).put(CssProperty.SCROLL_MARKER_GROUP, position);
    return self();
  }

  /// `overscroll-behavior-x`.
  default SELF overscrollBehaviorX(Overscroll behavior) {
    StyleTarget.declarations(this).put(CssProperty.OVERSCROLL_BEHAVIOR_X, behavior);
    return self();
  }

  /// `scrollbar-width: none`, hiding the scrollbar while keeping the element scrollable.
  default SELF scrollbarWidthNone() {
    StyleTarget.declarations(this).put(CssProperty.SCROLLBAR_WIDTH, "none");
    return self();
  }
}
