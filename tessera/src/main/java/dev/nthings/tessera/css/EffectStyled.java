package dev.nthings.tessera.css;

/// @param <SELF> concrete element or style builder type
public interface EffectStyled<SELF extends EffectStyled<SELF>> extends StyleTarget<SELF> {

  /// A preset that moves an element belongs in a `prefers-reduced-motion: no-preference` block;
  /// the visible end state stays outside it.
  default SELF motion(Motion motion) {
    motion.apply(style());
    return self();
  }

  default SELF transform(Transform transform) {
    style().put(CssProperty.TRANSFORM, transform);
    return self();
  }

  /// Uses the independent `translate` property, not `transform`.
  default SELF translate(Translation translation) {
    style().put(CssProperty.TRANSLATE, translation);
    return self();
  }

  default SELF backdropFilter(CssFilter filter) {
    style().put(CssProperty.BACKDROP_FILTER, filter);
    return self();
  }

  default SELF cursor(Cursor cursor) {
    style().put(CssProperty.CURSOR, cursor);
    return self();
  }

  default SELF cursorPointer() {
    return cursor(Cursor.POINTER);
  }

  /// Cross-document view transition name.
  default SELF viewTransitionName(CssIdent name) {
    style().put(CssProperty.VIEW_TRANSITION_NAME, name);
    return self();
  }

  default SELF viewTransitionClass(CssIdent name) {
    style().put(CssProperty.VIEW_TRANSITION_CLASS, name);
    return self();
  }

  /// Scroll-driven view timeline.
  default SELF viewTimeline(CssIdent name, TimelineAxis axis) {
    style().put(CssProperty.VIEW_TIMELINE, name.toCss() + " " + axis.toCss());
    return self();
  }

  /// Delays each element by `stepMilliseconds` per sibling index, starting at 0 for the first.
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

  /// Plays the keyframes `name` once over the whole root scroll range on `axis`.
  default SELF scrollProgressAnimation(CssIdent keyframes, TimelineAxis axis) {
    style().put(CssProperty.ANIMATION, keyframes.toCss() + " auto linear");
    style().put(CssProperty.ANIMATION_TIMELINE, "scroll(root " + axis.toCss() + ")");
    return self();
  }
}
