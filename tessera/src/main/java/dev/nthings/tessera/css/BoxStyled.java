package dev.nthings.tessera.css;

/// Spacing, background, borders, shadows, outlines, and form-control presentation.
///
/// @param <SELF> concrete element or style builder type
public interface BoxStyled<SELF extends BoxStyled<SELF>> extends StyleTarget<SELF> {

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

  default SELF ms(CssLength length) {
    style().put(CssProperty.MARGIN_INLINE_START, length);
    return self();
  }

  default SELF me(CssLength length) {
    style().put(CssProperty.MARGIN_INLINE_END, length);
    return self();
  }

  /// A solid background color.
  default SELF bg(CssColor color) {
    style().put(CssProperty.BACKGROUND, color);
    return self();
  }

  /// A background of image layers over a fill color.
  default SELF background(Background background) {
    style().put(CssProperty.BACKGROUND, background);
    return self();
  }

  default SELF border(Border border) {
    style().put(CssProperty.BORDER, border);
    return self();
  }

  default SELF borderNone() {
    return border(Border.none());
  }

  default SELF borderBlockStart(Border border) {
    style().put(CssProperty.BORDER_BLOCK_START, border);
    return self();
  }

  default SELF borderBlockEnd(Border border) {
    style().put(CssProperty.BORDER_BLOCK_END, border);
    return self();
  }

  default SELF borderInlineStart(Border border) {
    style().put(CssProperty.BORDER_INLINE_START, border);
    return self();
  }

  /// A hairline border in `color` on all sides.
  default SELF borderSubtle(CssColor color) {
    return border(Border.hairline(color));
  }

  default SELF borderBlockStartSubtle(CssColor color) {
    return borderBlockStart(Border.hairline(color));
  }

  default SELF borderBlockEndSubtle(CssColor color) {
    return borderBlockEnd(Border.hairline(color));
  }

  default SELF borderInlineStartSubtle(CssColor color) {
    return borderInlineStart(Border.hairline(color));
  }

  default SELF borderColor(CssColor color) {
    style().put(CssProperty.BORDER_COLOR, color);
    return self();
  }

  /// Separate, gapless table borders.
  default SELF borderSeparateZero() {
    style().put(CssProperty.BORDER_COLLAPSE, "separate");
    style().put(CssProperty.BORDER_SPACING, CssLength.zero());
    return self();
  }

  default SELF rounded(CssLength radius) {
    style().put(CssProperty.BORDER_RADIUS, radius);
    return self();
  }

  /// Box shadow layers, first on top.
  default SELF shadow(Shadow first, Shadow... rest) {
    style().put(CssProperty.BOX_SHADOW, first, rest);
    return self();
  }

  default SELF outline(Border outline) {
    style().put(CssProperty.OUTLINE, outline);
    return self();
  }

  default SELF outlineNone() {
    return outline(Border.none());
  }

  default SELF outlineOffset(CssLength offset) {
    style().put(CssProperty.OUTLINE_OFFSET, offset);
    return self();
  }

  /// A 2px outline in `accent`, offset 2px from the border edge.
  default SELF focusRing(CssColor accent) {
    return outline(Border.solid(CssLength.px(2), accent)).outlineOffset(CssLength.px(2));
  }

  default SELF opacity(double value) {
    style().put(CssProperty.OPACITY, CssLength.formatNumber(value));
    return self();
  }

  default SELF accentColor(CssColor color) {
    style().put(CssProperty.ACCENT_COLOR, color);
    return self();
  }

  default SELF appearance(Appearance appearance) {
    style().put(CssProperty.APPEARANCE, appearance);
    return self();
  }

  default SELF appearanceBaseSelect() {
    return appearance(Appearance.BASE_SELECT);
  }

  default SELF appearanceNone() {
    return appearance(Appearance.NONE);
  }

  default SELF resize(Resize axes) {
    style().put(CssProperty.RESIZE, axes);
    return self();
  }

  default SELF resizeVertical() {
    return resize(Resize.VERTICAL);
  }

  default SELF resizeNone() {
    return resize(Resize.NONE);
  }

  /// `field-sizing: content`: form controls grow with their content instead of fixed rows/cols.
  default SELF fieldSizingContent() {
    style().put(CssProperty.FIELD_SIZING, "content");
    return self();
  }
}
