package dev.nthings.tessera.css;

import dev.nthings.tessera.internal.foundation.Decimals;

/// Spacing, background, borders, shadows, outlines, and form-control presentation. Part of
/// [Styled].
///
/// @param <SELF> concrete element or style builder type
interface BoxStyled<SELF extends BoxStyled<SELF>> extends StyleTarget<SELF> {

  /// Padding on all sides: [#px(CssLength)] and [#py(CssLength)].
  default SELF p(CssLength length) {
    return px(length).py(length);
  }

  /// `padding-inline`. Removes [#ps(CssLength)] and [#pe(CssLength)].
  default SELF px(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.PADDING_INLINE, length);
    return self();
  }

  /// `padding-block`. Removes [#pt(CssLength)] and [#pb(CssLength)].
  default SELF py(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.PADDING_BLOCK, length);
    return self();
  }

  /// `padding-block-start`.
  default SELF pt(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.PADDING_BLOCK_START, length);
    return self();
  }

  /// `padding-block-end`.
  default SELF pb(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.PADDING_BLOCK_END, length);
    return self();
  }

  /// `padding-inline-start`.
  default SELF ps(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.PADDING_INLINE_START, length);
    return self();
  }

  /// `padding-inline-end`.
  default SELF pe(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.PADDING_INLINE_END, length);
    return self();
  }

  /// Margin on all sides: [#mx(CssLength)] and [#my(CssLength)].
  default SELF m(CssLength length) {
    return mx(length).my(length);
  }

  /// `margin-inline`. Removes [#ms(CssLength)] and [#me(CssLength)].
  default SELF mx(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.MARGIN_INLINE, length);
    return self();
  }

  /// `margin-block`. Removes [#mt(CssLength)] and [#mb(CssLength)].
  default SELF my(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.MARGIN_BLOCK, length);
    return self();
  }

  /// `margin-block-start`.
  default SELF mt(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.MARGIN_BLOCK_START, length);
    return self();
  }

  /// `margin-block-end`.
  default SELF mb(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.MARGIN_BLOCK_END, length);
    return self();
  }

  /// `margin-inline-start`.
  default SELF ms(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.MARGIN_INLINE_START, length);
    return self();
  }

  /// `margin-inline-end`.
  default SELF me(CssLength length) {
    StyleTarget.declarations(this).put(CssProperty.MARGIN_INLINE_END, length);
    return self();
  }

  /// `background` set to a single color.
  default SELF bg(CssColor color) {
    StyleTarget.declarations(this).put(CssProperty.BACKGROUND, color);
    return self();
  }

  /// Sets the background to a single soft-edged dot over a fill color.
  default SELF background(Background background) {
    StyleTarget.declarations(this).put(CssProperty.BACKGROUND, background);
    return self();
  }

  /// Sets the border on all four sides; [Border#none()] removes it and [Border#hairline(CssColor)]
  /// is the usual 1px divider. Removes [#borderColor(CssColor)] and the side borders.
  default SELF border(Border border) {
    StyleTarget.declarations(this).put(CssProperty.BORDER, border);
    return self();
  }

  /// `border-block-start`.
  default SELF borderBlockStart(Border border) {
    StyleTarget.declarations(this).put(CssProperty.BORDER_BLOCK_START, border);
    return self();
  }

  /// `border-block-end`.
  default SELF borderBlockEnd(Border border) {
    StyleTarget.declarations(this).put(CssProperty.BORDER_BLOCK_END, border);
    return self();
  }

  /// `border-inline-start`.
  default SELF borderInlineStart(Border border) {
    StyleTarget.declarations(this).put(CssProperty.BORDER_INLINE_START, border);
    return self();
  }

  /// `border-inline-end`.
  default SELF borderInlineEnd(Border border) {
    StyleTarget.declarations(this).put(CssProperty.BORDER_INLINE_END, border);
    return self();
  }

  /// `border-color` on all four sides. A side border keeps its own color whatever the call order.
  default SELF borderColor(CssColor color) {
    StyleTarget.declarations(this).put(CssProperty.BORDER_COLOR, color);
    return self();
  }

  /// Sets `border-collapse: separate` with zero `border-spacing`, for gapless table borders.
  default SELF borderSeparateZero() {
    StyleTarget.declarations(this)
        .put(CssProperty.BORDER_COLLAPSE, "separate")
        .put(CssProperty.BORDER_SPACING, CssLength.zero());
    return self();
  }

  /// `border-radius` on all corners.
  default SELF rounded(CssLength radius) {
    StyleTarget.declarations(this).put(CssProperty.BORDER_RADIUS, radius);
    return self();
  }

  /// Box shadow layers, first on top.
  default SELF shadow(Shadow first, Shadow... rest) {
    StyleTarget.declarations(this).put(CssProperty.BOX_SHADOW, first, rest);
    return self();
  }

  /// `outline`; [Border#none()] removes it.
  default SELF outline(Border outline) {
    StyleTarget.declarations(this).put(CssProperty.OUTLINE, outline);
    return self();
  }

  /// `outline-offset`, the gap between the border edge and the outline.
  default SELF outlineOffset(CssLength offset) {
    StyleTarget.declarations(this).put(CssProperty.OUTLINE_OFFSET, offset);
    return self();
  }

  /// A 2px outline in `accent`, offset 2px from the border edge.
  default SELF focusRing(CssColor accent) {
    return outline(Border.solid(CssLength.px(2), accent)).outlineOffset(CssLength.px(2));
  }

  /// `opacity`, from 0 (transparent) to 1 (opaque).
  default SELF opacity(double value) {
    StyleTarget.declarations(this).put(CssProperty.OPACITY, Decimals.format(value));
    return self();
  }

  /// `accent-color`, the tint of checkboxes, radios, range inputs and progress bars.
  default SELF accentColor(CssColor color) {
    StyleTarget.declarations(this).put(CssProperty.ACCENT_COLOR, color);
    return self();
  }

  /// `appearance`: [Appearance#NONE] drops native control rendering, and [Appearance#BASE_SELECT]
  /// opts a `<select>` into the stylable base rendering.
  default SELF appearance(Appearance appearance) {
    StyleTarget.declarations(this).put(CssProperty.APPEARANCE, appearance);
    return self();
  }

  /// `resize`: which axes the user can drag to resize the element, such as a `<textarea>`.
  default SELF resize(Resize axes) {
    StyleTarget.declarations(this).put(CssProperty.RESIZE, axes);
    return self();
  }

  /// Emits `field-sizing: content`: form controls grow with their content.
  default SELF fieldSizingContent() {
    StyleTarget.declarations(this).put(CssProperty.FIELD_SIZING, "content");
    return self();
  }
}
