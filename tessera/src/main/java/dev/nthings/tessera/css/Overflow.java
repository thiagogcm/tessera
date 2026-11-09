package dev.nthings.tessera.css;

/// How content that does not fit its box is shown, set with [Styled#overflow(Overflow)] and its
/// per-axis variants.
public enum Overflow implements CssKeyword {
  VISIBLE,
  HIDDEN,
  /// Like `HIDDEN`, but the box is not a scroll container, so it cannot be scrolled
  /// programmatically and `position: sticky` descendants still stick to an outer scroller.
  CLIP,
  SCROLL,
  AUTO
}
