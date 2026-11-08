package dev.nthings.tessera.css;

import java.util.List;

/// A transition: the properties it animates, with a duration, an easing, and whether discrete
/// values such as `display` transition too. Applied with [EffectStyled#motion(Motion)]. Spatial
/// presets belong inside `motionSafe`; keep the visible end state outside it, so the control is
/// in place even when no transition runs.
public record Motion(
    CssDuration duration, Easing easing, List<Animatable> properties, boolean allowDiscrete) {

  public Motion {
    if (properties.isEmpty()) {
      throw new IllegalArgumentException("A motion transitions at least one property");
    }
    properties = List.copyOf(properties);
  }

  /// Transitions `properties` over `duration` with `easing`. Discrete values do not transition.
  /// Throws IllegalArgumentException if no properties are given.
  public static Motion of(CssDuration duration, Easing easing, Animatable... properties) {
    return new Motion(duration, easing, List.of(properties), false);
  }

  /// Control colors: background, text, border and outline color over
  /// [Tokens#CONTROL_FEEDBACK_DURATION] (150ms) with `ease`. Discrete values do not transition.
  public static Motion controlFeedback() {
    return of(
        Tokens.CONTROL_FEEDBACK_DURATION,
        Easing.ease(),
        Animatable.BACKGROUND,
        Animatable.COLOR,
        Animatable.BORDER_COLOR,
        Animatable.OUTLINE_COLOR);
  }

  /// Overlay entry: opacity, transform, translate, `display` and overlay over
  /// [Tokens#OVERLAY_DURATION] (200ms) with `ease-out`. Discrete values transition, so an overlay
  /// can animate in from `display: none`.
  public static Motion overlayEntry() {
    return of(
            Tokens.OVERLAY_DURATION,
            Easing.easeOut(),
            Animatable.OPACITY,
            Animatable.TRANSFORM,
            Animatable.TRANSLATE,
            Animatable.DISPLAY,
            Animatable.OVERLAY)
        .allowingDiscrete();
  }

  /// Disclosure: block size and `content-visibility` over [Tokens#DISCLOSURE_DURATION] (250ms)
  /// with `ease`. Discrete values transition, so hidden content can animate open.
  public static Motion disclosure() {
    return of(
            Tokens.DISCLOSURE_DURATION,
            Easing.ease(),
            Animatable.BLOCK_SIZE,
            Animatable.CONTENT_VISIBILITY)
        .allowingDiscrete();
  }

  /// The same duration, easing and discrete setting over `replacement` properties. Throws
  /// IllegalArgumentException if none are given.
  public Motion properties(Animatable... replacement) {
    return new Motion(duration, easing, List.of(replacement), allowDiscrete);
  }

  /// Also transitions properties that switch between discrete values, such as `display`.
  public Motion allowingDiscrete() {
    return new Motion(duration, easing, properties, true);
  }

  /// Writes the transition declarations to `style`; [EffectStyled#motion(Motion)] calls it.
  public StyleRefinement apply(StyleRefinement style) {
    style
        .put(CssProperty.TRANSITION_PROPERTY, properties)
        .put(CssProperty.TRANSITION_DURATION, duration)
        .put(CssProperty.TRANSITION_TIMING_FUNCTION, easing);
    if (allowDiscrete) {
      style.put(CssProperty.TRANSITION_BEHAVIOR, "allow-discrete");
    }
    return style;
  }
}
