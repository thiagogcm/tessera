package dev.nthings.tessera.css;

/// Motion, transforms, filters, cursors, animation, and view transitions. Part of [Styled].
///
/// @param <SELF> concrete element or style builder type
interface EffectStyled<SELF extends EffectStyled<SELF>> extends StyleTarget<SELF> {

  /// Sets `transition-property`, `transition-duration`, and `transition-timing-function` from
  /// `motion`, plus `transition-behavior: allow-discrete` when it allows discrete transitions. In
  /// scoped CSS, a preset that moves an element belongs inside [ScopeStyle#motionSafe], with its
  /// visible end state kept outside that block.
  default SELF motion(Motion motion) {
    motion.apply(style());
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

  /// Sets `view-transition-name`, which pairs this element across a view transition.
  default SELF viewTransitionName(CssIdent name) {
    style().put(CssProperty.VIEW_TRANSITION_NAME, name);
    return self();
  }

  /// Sets `view-transition-class`, which groups transition names for shared styling.
  default SELF viewTransitionClass(CssIdent name) {
    style().put(CssProperty.VIEW_TRANSITION_CLASS, name);
    return self();
  }

  /// Delays each element by `stepMilliseconds` per sibling position, so the first sibling has no
  /// delay. Write it after any animation it delays: an animation written later replaces the delay.
  /// Throws IllegalArgumentException if `stepMilliseconds` is negative.
  default SELF animationStaggerBySiblingIndex(int stepMilliseconds) {
    if (stepMilliseconds < 0) {
      throw new IllegalArgumentException("stepMilliseconds must be >= 0");
    }
    style()
        .put(
            CssProperty.ANIMATION_DELAY,
            "calc((sibling-index() - 1) * " + stepMilliseconds + "ms)");
    return self();
  }

  /// Plays the `@keyframes` named `keyframes` once over the whole root scroll range along `axis`.
  /// This call does not register the keyframes; define them with
  /// [Interactivity#registerKeyframeRule(String)].
  default SELF scrollProgressAnimation(CssIdent keyframes, TimelineAxis axis) {
    style().put(CssProperty.ANIMATION, keyframes.toCss() + " auto linear");
    style().put(CssProperty.ANIMATION_TIMELINE, "scroll(root " + axis.toCss() + ")");
    return self();
  }
}
