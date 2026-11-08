package dev.nthings.tessera.css;

/// CSS anchor positioning: naming an anchor and placing an element against one.
public interface AnchorStyled<SELF extends AnchorStyled<SELF>> extends StyleTarget<SELF> {

  /// Names this element as an anchor, emitting `anchor-name: --name`. Throws
  /// IllegalArgumentException unless `name` is a dashed identifier, such as one from
  /// [CssIdent#custom].
  default SELF anchorName(CssIdent name) {
    style().put(CssProperty.ANCHOR_NAME, name.requireCustom());
    return self();
  }

  /// Places this element against the anchor `name`, emitting `position-anchor: --name`. Throws
  /// IllegalArgumentException unless `name` is a dashed identifier, such as one from
  /// [CssIdent#custom].
  default SELF positionAnchor(CssIdent name) {
    style().put(CssProperty.POSITION_ANCHOR, name.requireCustom());
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
