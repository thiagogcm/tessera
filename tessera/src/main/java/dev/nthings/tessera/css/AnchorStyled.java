package dev.nthings.tessera.css;

/// CSS anchor positioning: naming an anchor and placing an element against one.
///
/// @param <SELF> concrete element or style builder type
public interface AnchorStyled<SELF extends AnchorStyled<SELF>> extends StyleTarget<SELF> {

  /// Declares this element as an anchor under a dashed identifier.
  default SELF anchorName(CssIdent name) {
    style().put(CssProperty.ANCHOR_NAME, name.requireCustom());
    return self();
  }

  /// Positions this element against the anchor `name`.
  default SELF positionAnchor(CssIdent name) {
    style().put(CssProperty.POSITION_ANCHOR, name.requireCustom());
    return self();
  }

  default SELF positionArea(PositionArea area) {
    style().put(CssProperty.POSITION_AREA, area);
    return self();
  }

  /// Fallback placements tried in order when the element would overflow.
  default SELF positionTryFallbacks(PositionTry first, PositionTry... rest) {
    style().put(CssProperty.POSITION_TRY_FALLBACKS, first, rest);
    return self();
  }
}
