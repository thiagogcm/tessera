package dev.nthings.tessera.css;

/**
 * Canonical CSS property keys in deterministic ordinal order for {@link java.util.EnumMap} storage
 * and FNV-1a hashing.
 *
 * <p>Note:
 *
 * <ul>
 *   <li>{@link #TRANSITION} precedes {@link #TRANSITION_PROPERTY}, {@link #TRANSITION_DURATION},
 *       {@link #TRANSITION_TIMING_FUNCTION}, and {@link #TRANSITION_BEHAVIOR} so {@code
 *       transition-behavior: allow-discrete} is never reset by the shorthand.
 *   <li>{@link #ANIMATION} precedes {@link #ANIMATION_DELAY}, {@link #ANIMATION_TIMELINE}, and
 *       {@link #ANIMATION_RANGE} so the {@code animation} shorthand never resets scroll-driven
 *       timeline or range declarations.
 * </ul>
 */
public enum CssProperty {
  CONTENT("content"),
  DISPLAY("display"),
  POSITION("position"),
  INSET("inset"),
  INSET_BLOCK_START("inset-block-start"),
  INSET_BLOCK_END("inset-block-end"),
  INSET_INLINE_START("inset-inline-start"),
  INSET_INLINE_END("inset-inline-end"),
  Z_INDEX("z-index"),
  OVERFLOW("overflow"),
  OVERFLOW_X("overflow-x"),
  OVERFLOW_Y("overflow-y"),
  OVERFLOW_ANCHOR("overflow-anchor"),
  SCROLL_BEHAVIOR("scroll-behavior"),
  SCROLL_SNAP_TYPE("scroll-snap-type"),
  SCROLL_SNAP_ALIGN("scroll-snap-align"),
  SCROLL_MARGIN_BLOCK_START("scroll-margin-block-start"),
  SCROLL_TARGET_GROUP("scroll-target-group"),
  SCROLL_MARKER_GROUP("scroll-marker-group"),
  OVERSCROLL_BEHAVIOR_X("overscroll-behavior-x"),
  SCROLLBAR_WIDTH("scrollbar-width"),
  LIST_STYLE("list-style"),
  COUNTER_RESET("counter-reset"),
  COUNTER_INCREMENT("counter-increment"),
  FLEX_DIRECTION("flex-direction"),
  FLEX_WRAP("flex-wrap"),
  ALIGN_ITEMS("align-items"),
  JUSTIFY_CONTENT("justify-content"),
  FLEX_GROW("flex-grow"),
  FLEX_SHRINK("flex-shrink"),
  GRID_TEMPLATE_COLUMNS("grid-template-columns"),
  GRID_TEMPLATE_ROWS("grid-template-rows"),
  GRID_COLUMN("grid-column"),
  GRID_ROW("grid-row"),
  GAP("gap"),
  ROW_GAP("row-gap"),
  COLUMN_GAP("column-gap"),
  INLINE_SIZE("inline-size"),
  BLOCK_SIZE("block-size"),
  MIN_INLINE_SIZE("min-inline-size"),
  MAX_INLINE_SIZE("max-inline-size"),
  MIN_BLOCK_SIZE("min-block-size"),
  MAX_BLOCK_SIZE("max-block-size"),
  ASPECT_RATIO("aspect-ratio"),
  OBJECT_FIT("object-fit"),
  PADDING_INLINE("padding-inline"),
  PADDING_BLOCK("padding-block"),
  PADDING_BLOCK_START("padding-block-start"),
  PADDING_INLINE_START("padding-inline-start"),
  MARGIN_INLINE("margin-inline"),
  MARGIN_BLOCK("margin-block"),
  MARGIN_BLOCK_START("margin-block-start"),
  MARGIN_BLOCK_END("margin-block-end"),
  MARGIN_INLINE_START("margin-inline-start"),
  MARGIN_INLINE_END("margin-inline-end"),
  BACKGROUND("background"),
  BACKDROP_FILTER("backdrop-filter"),
  COLOR("color"),
  FONT_SIZE("font-size"),
  FONT_WEIGHT("font-weight"),
  FONT_VARIANT_NUMERIC("font-variant-numeric"),
  LINE_HEIGHT("line-height"),
  TEXT_ALIGN("text-align"),
  TEXT_WRAP("text-wrap"),
  TEXT_DECORATION_LINE("text-decoration-line"),
  TEXT_DECORATION_STYLE("text-decoration-style"),
  TEXT_DECORATION_COLOR("text-decoration-color"),
  TEXT_DECORATION_THICKNESS("text-decoration-thickness"),
  TEXT_UNDERLINE_OFFSET("text-underline-offset"),
  TEXT_DECORATION_SKIP_INK("text-decoration-skip-ink"),
  BORDER("border"),
  BORDER_BLOCK_START("border-block-start"),
  BORDER_BLOCK_END("border-block-end"),
  BORDER_INLINE_START("border-inline-start"),
  BORDER_COLLAPSE("border-collapse"),
  BORDER_SPACING("border-spacing"),
  BORDER_RADIUS("border-radius"),
  BOX_SHADOW("box-shadow"),
  OUTLINE("outline"),
  OUTLINE_OFFSET("outline-offset"),
  OPACITY("opacity"),
  TRANSFORM("transform"),
  TRANSLATE("translate"),
  CURSOR("cursor"),
  TRANSITION("transition"),
  TRANSITION_PROPERTY("transition-property"),
  TRANSITION_DURATION("transition-duration"),
  TRANSITION_TIMING_FUNCTION("transition-timing-function"),
  TRANSITION_BEHAVIOR("transition-behavior"),
  VIEW_TIMELINE("view-timeline"),
  SCROLL_TIMELINE("scroll-timeline"),
  TIMELINE_SCOPE("timeline-scope"),
  ANIMATION("animation"),
  ANIMATION_DELAY("animation-delay"),
  ANIMATION_TIMELINE("animation-timeline"),
  ANIMATION_RANGE("animation-range"),
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
