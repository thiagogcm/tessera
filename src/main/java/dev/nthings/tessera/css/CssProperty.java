package dev.nthings.tessera.css;

/**
 * Canonical CSS property keys in deterministic ordinal order for {@link java.util.EnumMap} storage
 * and FNV-1a hashing.
 */
public enum CssProperty {
  DISPLAY("display"),
  POSITION("position"),
  FLEX_DIRECTION("flex-direction"),
  FLEX_WRAP("flex-wrap"),
  ALIGN_ITEMS("align-items"),
  JUSTIFY_CONTENT("justify-content"),
  FLEX_GROW("flex-grow"),
  FLEX_SHRINK("flex-shrink"),
  GRID_TEMPLATE_COLUMNS("grid-template-columns"),
  GAP("gap"),
  ROW_GAP("row-gap"),
  COLUMN_GAP("column-gap"),
  INLINE_SIZE("inline-size"),
  BLOCK_SIZE("block-size"),
  MIN_INLINE_SIZE("min-inline-size"),
  MAX_INLINE_SIZE("max-inline-size"),
  MIN_BLOCK_SIZE("min-block-size"),
  PADDING_INLINE("padding-inline"),
  PADDING_BLOCK("padding-block"),
  MARGIN_INLINE("margin-inline"),
  MARGIN_BLOCK("margin-block"),
  BACKGROUND("background"),
  COLOR("color"),
  FONT_SIZE("font-size"),
  FONT_WEIGHT("font-weight"),
  LINE_HEIGHT("line-height"),
  TEXT_ALIGN("text-align"),
  TEXT_WRAP("text-wrap"),
  BORDER("border"),
  BORDER_RADIUS("border-radius"),
  BOX_SHADOW("box-shadow"),
  OUTLINE("outline"),
  OUTLINE_OFFSET("outline-offset"),
  OPACITY("opacity"),
  CURSOR("cursor"),
  TRANSITION("transition"),
  CONTAINER_NAME("container-name"),
  CONTAINER_TYPE("container-type"),
  ANCHOR_NAME("anchor-name"),
  POSITION_ANCHOR("position-anchor"),
  POSITION_AREA("position-area"),
  POSITION_TRY_FALLBACKS("position-try-fallbacks"),
  VIEW_TRANSITION_NAME("view-transition-name"),
  VIEW_TRANSITION_CLASS("view-transition-class"),
  CONTENT_VISIBILITY("content-visibility"),
  CONTAIN_INTRINSIC_SIZE("contain-intrinsic-size");

  private final String cssName;

  CssProperty(String cssName) {
    this.cssName = cssName;
  }

  public String cssName() {
    return cssName;
  }
}
