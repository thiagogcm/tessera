package dev.nthings.tessera.css;

/// Transitions, animations, transforms, filters and cursors. Part of [Styled].
///
/// @param <SELF> concrete element or style builder type
interface EffectStyled<SELF extends EffectStyled<SELF>> extends StyleTarget<SELF> {

  /// Sets `transition-property`, `transition-duration`, and `transition-timing-function` from
  /// `transition`, plus `transition-behavior: allow-discrete` when it allows discrete transitions.
  /// A transition that moves an element belongs inside `on(Media.MOTION_SAFE, ...)`, with its
  /// visible end state kept outside that block.
  default SELF transition(Transition transition) {
    transition.writeTo(style());
    return self();
  }

  /// Plays `animation`, under `prefers-reduced-motion: no-preference` only, and registers its
  /// keyframes in the stylesheet. A later animation on the same style replaces it.
  default SELF animation(Animation animation) {
    style().animate(animation);
    return self();
  }

  /// `transform`.
  default SELF transform(Transform transform) {
    style().put(CssProperty.TRANSFORM, transform);
    return self();
  }

  /// Uses the independent `translate` property, not `transform`.
  default SELF translate(Translation translation) {
    style().put(CssProperty.TRANSLATE, translation);
    return self();
  }

  /// `backdrop-filter`, applied to what shows through a translucent background.
  default SELF backdropFilter(CssFilter filter) {
    style().put(CssProperty.BACKDROP_FILTER, filter);
    return self();
  }

  /// `cursor`, the pointer shown over the element.
  default SELF cursor(Cursor cursor) {
    style().put(CssProperty.CURSOR, cursor);
    return self();
  }
}
