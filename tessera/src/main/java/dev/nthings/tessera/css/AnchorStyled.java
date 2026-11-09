package dev.nthings.tessera.css;

/// CSS anchor positioning: naming an anchor and placing an element against one. Part of [Styled].
///
/// @param <SELF> concrete element or style builder type
interface AnchorStyled<SELF extends AnchorStyled<SELF>> extends StyleTarget<SELF> {

  /// Names this element as `anchor`, emitting `anchor-name: --name`.
  default SELF anchorName(Anchor anchor) {
    style().put(CssProperty.ANCHOR_NAME, anchor);
    return self();
  }

  /// Places this element against `anchor`, emitting `position-anchor: --name`.
  default SELF positionAnchor(Anchor anchor) {
    style().put(CssProperty.POSITION_ANCHOR, anchor);
    return self();
  }

  /// Sets the region around the position anchor where the element is placed, emitting
  /// `position-area`.
  default SELF positionArea(PositionArea area) {
    style().put(CssProperty.POSITION_AREA, area);
    return self();
  }

  /// Sets the [PositionTry] fallbacks, tried in order when the element overflows.
  default SELF positionTryFallbacks(PositionTry first, PositionTry... rest) {
    style().put(CssProperty.POSITION_TRY_FALLBACKS, first, rest);
    return self();
  }
}
