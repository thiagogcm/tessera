package dev.nthings.tessera.css;

import java.util.function.UnaryOperator;

/// Composable fluent layout and typography styling trait.
///
/// @param <SELF> concrete element or style builder type
public interface Styled<SELF extends Styled<SELF>> {

  SELF self();

  StyleRefinement style();

  default SELF refine(UnaryOperator<StyleRefinement> mutator) {
    mutator.apply(style());
    return self();
  }

  /// Applies a complete row or column. Later calls such as [#gap(CssLength)],
  /// [#blockSize(CssLength)], or [#flexShrink(int)] replace the matching declaration on this
  /// element.
  default SELF layout(FlexLayout layout) {
    layout.apply(style());
    return self();
  }

  default SELF flex() {
    style().put(CssProperty.DISPLAY, "flex");
    return self();
  }

  default SELF inlineFlex() {
    style().put(CssProperty.DISPLAY, "inline-flex");
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

  default SELF inset(String value) {
    style().put(CssProperty.INSET, value);
    return self();
  }

  default SELF insetBlockStart(CssLength length) {
    style().put(CssProperty.INSET_BLOCK_START, length);
    return self();
  }

  default SELF insetInlineStart(CssLength length) {
    style().put(CssProperty.INSET_INLINE_START, length);
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

  default SELF listStyleNone() {
    style().put(CssProperty.LIST_STYLE, "none");
    return self();
  }

  default SELF counterReset(String counterName) {
    style().put(CssProperty.COUNTER_RESET, counterName);
    return self();
  }

  default SELF counterIncrement(String counterName) {
    style().put(CssProperty.COUNTER_INCREMENT, counterName);
    return self();
  }

  default SELF content(String value) {
    style().put(CssProperty.CONTENT, value);
    return self();
  }

  default SELF flexRow() {
    style().put(CssProperty.FLEX_DIRECTION, "row");
    return self();
  }

  default SELF flexCol() {
    style().put(CssProperty.FLEX_DIRECTION, "column");
    return self();
  }

  default SELF flexWrap() {
    style().put(CssProperty.FLEX_WRAP, "wrap");
    return self();
  }

  default SELF itemsStart() {
    style().put(CssProperty.ALIGN_ITEMS, "flex-start");
    return self();
  }

  default SELF itemsCenter() {
    style().put(CssProperty.ALIGN_ITEMS, "center");
    return self();
  }

  default SELF itemsEnd() {
    style().put(CssProperty.ALIGN_ITEMS, "flex-end");
    return self();
  }

  default SELF itemsStretch() {
    style().put(CssProperty.ALIGN_ITEMS, "stretch");
    return self();
  }

  default SELF itemsBaseline() {
    style().put(CssProperty.ALIGN_ITEMS, "baseline");
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

  default SELF gridColumns(String template) {
    style().put(CssProperty.GRID_TEMPLATE_COLUMNS, template);
    return self();
  }

  default SELF gridRows(String template) {
    style().put(CssProperty.GRID_TEMPLATE_ROWS, template);
    return self();
  }

  default SELF subgridRows(int trackSpan) {
    if (trackSpan <= 0) {
      throw new IllegalArgumentException("trackSpan must be > 0");
    }
    return grid().gridRow("span " + trackSpan).gridRows("subgrid");
  }

  default SELF gridColumn(String spanSpec) {
    style().put(CssProperty.GRID_COLUMN, spanSpec);
    return self();
  }

  default SELF gridRow(String spanSpec) {
    style().put(CssProperty.GRID_ROW, spanSpec);
    return self();
  }

  default SELF colSpan(int columns) {
    if (columns <= 0) {
      throw new IllegalArgumentException("columns must be > 0");
    }
    return gridColumn("span " + columns);
  }

  default SELF rowSpan(int rows) {
    if (rows <= 0) {
      throw new IllegalArgumentException("rows must be > 0");
    }
    return gridRow("span " + rows);
  }

  default SELF gridAutoFit(CssLength minColumnWidth) {
    return grid().gridColumns(autoFitTracks(minColumnWidth));
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

  default SELF aspectRatio(String ratio) {
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

  default SELF px(CssLength length) {
    style().put(CssProperty.PADDING_INLINE, length);
    return self();
  }

  default SELF py(CssLength length) {
    style().put(CssProperty.PADDING_BLOCK, length);
    return self();
  }

  default SELF p(CssLength length) {
    return px(length).py(length);
  }

  default SELF pt(CssLength length) {
    style().put(CssProperty.PADDING_BLOCK_START, length);
    return self();
  }

  default SELF ps(CssLength length) {
    style().put(CssProperty.PADDING_INLINE_START, length);
    return self();
  }

  default SELF mx(CssLength length) {
    style().put(CssProperty.MARGIN_INLINE, length);
    return self();
  }

  default SELF my(CssLength length) {
    style().put(CssProperty.MARGIN_BLOCK, length);
    return self();
  }

  default SELF m(CssLength length) {
    return mx(length).my(length);
  }

  default SELF mt(CssLength length) {
    style().put(CssProperty.MARGIN_BLOCK_START, length);
    return self();
  }

  default SELF mb(CssLength length) {
    style().put(CssProperty.MARGIN_BLOCK_END, length);
    return self();
  }

  default SELF typography(Typography typography) {
    typography.apply(style());
    return self();
  }

  /// Applies a motion preset. A preset that moves an element belongs inside a
  /// `prefers-reduced-motion: no-preference` block; the visible end state stays outside it.
  default SELF motion(Motion motion) {
    motion.apply(style());
    return self();
  }

  default SELF bg(DesignToken.Color token) {
    style().put(CssProperty.BACKGROUND, token.varReference());
    return self();
  }

  default SELF bgRaw(String colorValue) {
    style().put(CssProperty.BACKGROUND, colorValue);
    return self();
  }

  default SELF textColor(DesignToken.Color token) {
    style().put(CssProperty.COLOR, token.varReference());
    return self();
  }

  /// Text color that contrasts with `background` (`contrast-color()`), e.g. for filled buttons.
  default SELF textColorOn(DesignToken.Color background) {
    style().put(CssProperty.COLOR, "contrast-color(" + background.varReference() + ")");
    return self();
  }

  default SELF textColorRaw(String colorValue) {
    style().put(CssProperty.COLOR, colorValue);
    return self();
  }

  default SELF fontSize(CssLength size) {
    style().put(CssProperty.FONT_SIZE, size);
    return self();
  }

  default SELF textDecorationNone() {
    style().put(CssProperty.TEXT_DECORATION_LINE, "none");
    return self();
  }

  default SELF fontFamily(String family) {
    style().put(CssProperty.FONT_FAMILY, family);
    return self();
  }

  default SELF fontWeight(int weight) {
    style().put(CssProperty.FONT_WEIGHT, Integer.toString(weight));
    return self();
  }

  default SELF tabularNums() {
    style().put(CssProperty.FONT_VARIANT_NUMERIC, "tabular-nums");
    return self();
  }

  default SELF lineHeight(String value) {
    style().put(CssProperty.LINE_HEIGHT, value);
    return self();
  }

  default SELF textAlign(String alignment) {
    style().put(CssProperty.TEXT_ALIGN, alignment);
    return self();
  }

  default SELF textBalance() {
    style().put(CssProperty.TEXT_WRAP, "balance");
    return self();
  }

  default SELF textPretty() {
    style().put(CssProperty.TEXT_WRAP, "pretty");
    return self();
  }

  default SELF highlight(HighlightTone tone) {
    style().put(CssProperty.BACKGROUND, tone.toCssBackground());
    style().put(CssProperty.PADDING_INLINE, Tokens.SPACE_1.length());
    style().put(CssProperty.BORDER_RADIUS, Tokens.RADIUS_SM.length());
    return self();
  }

  default SELF underline() {
    return underline(UnderlineStyle.SOLID, Tokens.ACCENT_PRIMARY);
  }

  default SELF underline(UnderlineStyle lineStyle, DesignToken.Color colorToken) {
    style().put(CssProperty.TEXT_DECORATION_LINE, "underline");
    style().put(CssProperty.TEXT_DECORATION_STYLE, lineStyle.css());
    style().put(CssProperty.TEXT_DECORATION_COLOR, colorToken.varReference());
    style().put(CssProperty.TEXT_DECORATION_THICKNESS, "0.1em");
    style().put(CssProperty.TEXT_UNDERLINE_OFFSET, CssLength.em(0.18));
    style().put(CssProperty.TEXT_DECORATION_SKIP_INK, "auto");
    return self();
  }

  default SELF strikethrough() {
    return strikethrough(Tokens.TEXT_MUTED);
  }

  default SELF strikethrough(DesignToken.Color colorToken) {
    style().put(CssProperty.TEXT_DECORATION_LINE, "line-through");
    style().put(CssProperty.TEXT_DECORATION_STYLE, UnderlineStyle.SOLID.css());
    style().put(CssProperty.TEXT_DECORATION_COLOR, colorToken.varReference());
    style().put(CssProperty.TEXT_DECORATION_THICKNESS, "0.09em");
    return self();
  }

  default SELF border(String borderSpec) {
    style().put(CssProperty.BORDER, borderSpec);
    return self();
  }

  default SELF borderSubtle(DesignToken.Color borderToken) {
    style().put(CssProperty.BORDER, "1px solid " + borderToken.varReference());
    return self();
  }

  default SELF borderBlockStartSubtle(DesignToken.Color borderToken) {
    style().put(CssProperty.BORDER_BLOCK_START, "1px solid " + borderToken.varReference());
    return self();
  }

  default SELF borderBlockEndSubtle(DesignToken.Color borderToken) {
    style().put(CssProperty.BORDER_BLOCK_END, "1px solid " + borderToken.varReference());
    return self();
  }

  default SELF borderSeparateZero() {
    style().put(CssProperty.BORDER_COLLAPSE, "separate");
    style().put(CssProperty.BORDER_SPACING, "0");
    return self();
  }

  default SELF rounded(CssLength radius) {
    style().put(CssProperty.BORDER_RADIUS, radius);
    return self();
  }

  default SELF shadow(String shadowSpec) {
    style().put(CssProperty.BOX_SHADOW, shadowSpec);
    return self();
  }

  default SELF focusRing(DesignToken.Color accentToken) {
    style().put(CssProperty.OUTLINE, "2px solid " + accentToken.varReference());
    style().put(CssProperty.OUTLINE_OFFSET, CssLength.px(2));
    return self();
  }

  default SELF opacity(double value) {
    style().put(CssProperty.OPACITY, CssLength.formatNumber(value));
    return self();
  }

  default SELF cursor(String value) {
    style().put(CssProperty.CURSOR, value);
    return self();
  }

  default SELF cursorPointer() {
    style().put(CssProperty.CURSOR, "pointer");
    return self();
  }

  default SELF transition(String value) {
    style().put(CssProperty.TRANSITION, value);
    return self();
  }

  default SELF containerInlineSize() {
    style().put(CssProperty.CONTAINER_TYPE, "inline-size");
    return self();
  }

  /// Named inline-size query container (`container: <name> / inline-size`).
  default SELF containerInlineSize(String containerName) {
    style().put(CssProperty.CONTAINER_NAME, containerName);
    return containerInlineSize();
  }

  default SELF contentVisibilityAuto(String intrinsicBlockSize) {
    style().put(CssProperty.CONTENT_VISIBILITY, "auto");
    style().put(CssProperty.CONTAIN_INTRINSIC_SIZE, "auto " + intrinsicBlockSize);
    return self();
  }

  default SELF contentVisibility(String mode) {
    style().put(CssProperty.CONTENT_VISIBILITY, mode);
    return self();
  }

  default SELF insetBlockEnd(CssLength length) {
    style().put(CssProperty.INSET_BLOCK_END, length);
    return self();
  }

  default SELF insetInlineEnd(CssLength length) {
    style().put(CssProperty.INSET_INLINE_END, length);
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

  default SELF overflowAnchorNone() {
    style().put(CssProperty.OVERFLOW_ANCHOR, "none");
    return self();
  }

  default SELF scrollBehaviorSmooth() {
    style().put(CssProperty.SCROLL_BEHAVIOR, "smooth");
    return self();
  }

  default SELF scrollSnapType(String spec) {
    style().put(CssProperty.SCROLL_SNAP_TYPE, spec);
    return self();
  }

  default SELF scrollSnapAlign(String align) {
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

  default SELF scrollMarkerGroup(String position) {
    style().put(CssProperty.SCROLL_MARKER_GROUP, position);
    return self();
  }

  default SELF overscrollBehaviorX(String mode) {
    style().put(CssProperty.OVERSCROLL_BEHAVIOR_X, mode);
    return self();
  }

  default SELF scrollbarWidthNone() {
    style().put(CssProperty.SCROLLBAR_WIDTH, "none");
    return self();
  }

  default SELF ms(CssLength length) {
    style().put(CssProperty.MARGIN_INLINE_START, length);
    return self();
  }

  default SELF me(CssLength length) {
    style().put(CssProperty.MARGIN_INLINE_END, length);
    return self();
  }

  default SELF borderInlineStart(String spec) {
    style().put(CssProperty.BORDER_INLINE_START, spec);
    return self();
  }

  default SELF borderInlineStartSubtle(DesignToken.Color borderToken) {
    style().put(CssProperty.BORDER_INLINE_START, "1px solid " + borderToken.varReference());
    return self();
  }

  default SELF borderNone() {
    style().put(CssProperty.BORDER, "none");
    return self();
  }

  default SELF backdropFilter(String filter) {
    style().put(CssProperty.BACKDROP_FILTER, filter);
    return self();
  }

  default SELF transform(String value) {
    style().put(CssProperty.TRANSFORM, value);
    return self();
  }

  default SELF translate(String value) {
    style().put(CssProperty.TRANSLATE, value);
    return self();
  }

  default SELF transitionProperty(String properties) {
    style().put(CssProperty.TRANSITION_PROPERTY, properties);
    return self();
  }

  default SELF transitionDuration(String duration) {
    style().put(CssProperty.TRANSITION_DURATION, duration);
    return self();
  }

  default SELF transitionTimingFunction(String easing) {
    style().put(CssProperty.TRANSITION_TIMING_FUNCTION, easing);
    return self();
  }

  default SELF transitionAllowDiscrete() {
    style().put(CssProperty.TRANSITION_BEHAVIOR, "allow-discrete");
    return self();
  }

  /// Anonymous `container-type: scroll-state` (queried by the nearest `scroll-state()` query).
  default SELF containerScrollState() {
    style().put(CssProperty.CONTAINER_TYPE, "scroll-state");
    return self();
  }

  default SELF containerScrollState(String containerName) {
    style().put(CssProperty.CONTAINER_NAME, containerName);
    style().put(CssProperty.CONTAINER_TYPE, "scroll-state");
    return self();
  }

  default SELF containerScrollStateInlineSize(String containerName) {
    style().put(CssProperty.CONTAINER_NAME, containerName);
    style().put(CssProperty.CONTAINER_TYPE, "scroll-state inline-size");
    return self();
  }

  default SELF anchorName(String customIdent) {
    style().put(CssProperty.ANCHOR_NAME, customIdent);
    return self();
  }

  default SELF positionAnchor(String customIdent) {
    style().put(CssProperty.POSITION_ANCHOR, customIdent);
    return self();
  }

  default SELF positionArea(String area) {
    style().put(CssProperty.POSITION_AREA, area);
    return self();
  }

  default SELF positionTryFallbacks(String fallbacks) {
    style().put(CssProperty.POSITION_TRY_FALLBACKS, fallbacks);
    return self();
  }

  default SELF whiteSpace(String mode) {
    style().put(CssProperty.WHITE_SPACE, mode);
    return self();
  }

  default SELF borderColor(DesignToken.Color token) {
    style().put(CssProperty.BORDER_COLOR, token.varReference());
    return self();
  }

  default SELF borderColor(String color) {
    style().put(CssProperty.BORDER_COLOR, color);
    return self();
  }

  default SELF appearance(String value) {
    style().put(CssProperty.APPEARANCE, value);
    return self();
  }

  default SELF appearanceBaseSelect() {
    return appearance("base-select");
  }

  default SELF appearanceNone() {
    return appearance("none");
  }

  default SELF accentColor(DesignToken.Color token) {
    style().put(CssProperty.ACCENT_COLOR, token.varReference());
    return self();
  }

  default SELF accentColor(String value) {
    style().put(CssProperty.ACCENT_COLOR, value);
    return self();
  }

  default SELF resize(String mode) {
    style().put(CssProperty.RESIZE, mode);
    return self();
  }

  /// `field-sizing: content`: form controls grow with their content instead of fixed rows/cols.
  default SELF fieldSizingContent() {
    style().put(CssProperty.FIELD_SIZING, "content");
    return self();
  }

  default SELF resizeVertical() {
    return resize("vertical");
  }

  default SELF resizeNone() {
    return resize("none");
  }

  default SELF outline(String value) {
    style().put(CssProperty.OUTLINE, value);
    return self();
  }

  default SELF outlineNone() {
    return outline("none");
  }

  default SELF outlineOffset(CssLength offset) {
    style().put(CssProperty.OUTLINE_OFFSET, offset);
    return self();
  }

  /// `grid-template-columns` value for responsive auto-fit tracks at least `minColumnWidth` wide.
  static String autoFitTracks(CssLength minColumnWidth) {
    return "repeat(auto-fit, minmax(min(100%, " + minColumnWidth.toCss() + "), 1fr))";
  }
}
