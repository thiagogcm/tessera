package dev.nthings.tessera.css;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/// The CSS properties [Declarations] can declare. Declarations are emitted in constant order, and
/// every shorthand precedes its longhands, so a longhand written after its shorthand wins in the
/// cascade. Writing a shorthand removes its longhands ([#longhands()]), so the last call wins
/// either way.
enum CssProperty {
  CONTENT,
  DISPLAY,
  POSITION,
  INSET,
  INSET_BLOCK_START,
  INSET_BLOCK_END,
  INSET_INLINE_START,
  INSET_INLINE_END,
  Z_INDEX,
  OVERFLOW,
  OVERFLOW_X,
  OVERFLOW_Y,
  OVERFLOW_ANCHOR,
  SCROLL_BEHAVIOR,
  SCROLL_SNAP_TYPE,
  SCROLL_SNAP_ALIGN,
  SCROLL_MARGIN_BLOCK_START,
  SCROLL_TARGET_GROUP,
  SCROLL_MARKER_GROUP,
  OVERSCROLL_BEHAVIOR_X,
  SCROLLBAR_WIDTH,
  LIST_STYLE,
  COUNTER_RESET,
  COUNTER_INCREMENT,
  FLEX_DIRECTION,
  FLEX_WRAP,
  ALIGN_ITEMS,
  JUSTIFY_CONTENT,
  FLEX_GROW,
  FLEX_SHRINK,
  GRID_TEMPLATE_COLUMNS,
  GRID_TEMPLATE_ROWS,
  GRID_COLUMN,
  GRID_ROW,
  GAP,
  ROW_GAP,
  COLUMN_GAP,
  INLINE_SIZE,
  BLOCK_SIZE,
  MIN_INLINE_SIZE,
  MAX_INLINE_SIZE,
  MIN_BLOCK_SIZE,
  MAX_BLOCK_SIZE,
  ASPECT_RATIO,
  OBJECT_FIT,
  PADDING_INLINE,
  PADDING_BLOCK,
  PADDING_BLOCK_START,
  PADDING_INLINE_START,
  PADDING_BLOCK_END,
  PADDING_INLINE_END,
  MARGIN_INLINE,
  MARGIN_BLOCK,
  MARGIN_BLOCK_START,
  MARGIN_BLOCK_END,
  MARGIN_INLINE_START,
  MARGIN_INLINE_END,
  BACKGROUND,
  BACKDROP_FILTER,
  COLOR,
  FONT_FAMILY,
  FONT_SIZE,
  FONT_WEIGHT,
  FONT_VARIANT_NUMERIC,
  LINE_HEIGHT,
  LETTER_SPACING,
  TEXT_ALIGN,
  TEXT_WRAP,
  WHITE_SPACE,
  TEXT_DECORATION_LINE,
  TEXT_DECORATION_STYLE,
  TEXT_DECORATION_COLOR,
  TEXT_DECORATION_THICKNESS,
  TEXT_UNDERLINE_OFFSET,
  TEXT_DECORATION_SKIP_INK,
  BORDER,
  BORDER_COLOR,
  BORDER_BLOCK_START,
  BORDER_BLOCK_END,
  BORDER_INLINE_START,
  BORDER_INLINE_END,
  BORDER_COLLAPSE,
  BORDER_SPACING,
  BORDER_RADIUS,
  BOX_SHADOW,
  OUTLINE,
  OUTLINE_OFFSET,
  OPACITY,
  TRANSFORM,
  TRANSLATE,
  CURSOR,
  TRANSITION_PROPERTY,
  TRANSITION_DURATION,
  TRANSITION_TIMING_FUNCTION,
  TRANSITION_BEHAVIOR,
  ANIMATION,
  ANIMATION_DELAY,
  ANIMATION_TIMELINE,
  ANIMATION_RANGE,
  CONTAINER_NAME,
  CONTAINER_TYPE,
  ANCHOR_NAME,
  POSITION_ANCHOR,
  POSITION_AREA,
  POSITION_TRY_FALLBACKS,
  VIEW_TRANSITION_NAME,
  VIEW_TRANSITION_CLASS,
  APPEARANCE,
  ACCENT_COLOR,
  RESIZE,
  FIELD_SIZING,
  TIMELINE_TRIGGER,
  ANIMATION_TRIGGER;

  private final String cssName = name().toLowerCase(Locale.ROOT).replace('_', '-');

  /// The kebab-case CSS property name, such as `padding-inline`.
  String cssName() {
    return cssName;
  }

  /// The declarable properties this shorthand sets or resets; empty for a longhand. `border-color`
  /// and the side borders overlap without either containing the other, so neither clears the other
  /// and a side border keeps its own color.
  List<CssProperty> longhands() {
    return LONGHANDS.get(ordinal());
  }

  /// Each constant's longhands, indexed by ordinal and computed once.
  private static final List<List<CssProperty>> LONGHANDS =
      Stream.of(values()).map(CssProperty::computeLonghands).toList();

  private static List<CssProperty> computeLonghands(CssProperty property) {
    return switch (property) {
      case INSET ->
          List.of(INSET_BLOCK_START, INSET_BLOCK_END, INSET_INLINE_START, INSET_INLINE_END);
      case OVERFLOW -> List.of(OVERFLOW_X, OVERFLOW_Y);
      case GAP -> List.of(ROW_GAP, COLUMN_GAP);
      case PADDING_INLINE -> List.of(PADDING_INLINE_START, PADDING_INLINE_END);
      case PADDING_BLOCK -> List.of(PADDING_BLOCK_START, PADDING_BLOCK_END);
      case MARGIN_INLINE -> List.of(MARGIN_INLINE_START, MARGIN_INLINE_END);
      case MARGIN_BLOCK -> List.of(MARGIN_BLOCK_START, MARGIN_BLOCK_END);
      case BORDER ->
          List.of(
              BORDER_COLOR,
              BORDER_BLOCK_START,
              BORDER_BLOCK_END,
              BORDER_INLINE_START,
              BORDER_INLINE_END);
      case ANIMATION -> List.of(ANIMATION_DELAY, ANIMATION_TIMELINE, ANIMATION_RANGE);
      default -> List.of();
    };
  }
}
