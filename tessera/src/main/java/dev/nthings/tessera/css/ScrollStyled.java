package dev.nthings.tessera.css;

/// Scroll behavior, snapping, and scrollbars.
///
/// @param <SELF> concrete element or style builder type
public interface ScrollStyled<SELF extends ScrollStyled<SELF>> extends StyleTarget<SELF> {

  default SELF overflowAnchorNone() {
    style().put(CssProperty.OVERFLOW_ANCHOR, "none");
    return self();
  }

  default SELF scrollBehaviorSmooth() {
    style().put(CssProperty.SCROLL_BEHAVIOR, "smooth");
    return self();
  }

  default SELF scrollSnapType(ScrollSnap snap) {
    style().put(CssProperty.SCROLL_SNAP_TYPE, snap);
    return self();
  }

  default SELF scrollSnapAlign(SnapAlign align) {
    style().put(CssProperty.SCROLL_SNAP_ALIGN, align);
    return self();
  }

  default SELF scrollMarginBlockStart(CssLength length) {
    style().put(CssProperty.SCROLL_MARGIN_BLOCK_START, length);
    return self();
  }

  default SELF scrollTargetGroupAuto() {
    style().put(CssProperty.SCROLL_TARGET_GROUP, "auto");
    return self();
  }

  default SELF scrollMarkerGroup(ScrollMarkerGroup position) {
    style().put(CssProperty.SCROLL_MARKER_GROUP, position);
    return self();
  }

  default SELF overscrollBehaviorX(Overscroll behavior) {
    style().put(CssProperty.OVERSCROLL_BEHAVIOR_X, behavior);
    return self();
  }

  default SELF scrollbarWidthNone() {
    style().put(CssProperty.SCROLLBAR_WIDTH, "none");
    return self();
  }
}
