package dev.nthings.tessera.css;

import com.google.errorprone.annotations.CheckReturnValue;
import java.util.List;

/// A transition: the properties it animates, with a duration, an easing, and whether discrete
/// values such as `display` transition too. Applied with [Styled#transition(Transition)]. A
/// transition that moves an element belongs inside `on(Media.MOTION_SAFE, ...)`; keep the visible
/// end state outside it, so the control is in place even when no transition runs.
@CheckReturnValue
public record Transition(
    CssDuration duration, Easing easing, List<Animatable> properties, boolean allowDiscrete) {

  public Transition {
    if (properties.isEmpty()) {
      throw new IllegalArgumentException("A transition animates at least one property");
    }
    properties = List.copyOf(properties);
  }

  /// Transitions `properties` over `duration` with `easing`. Discrete values do not transition.
  /// Throws IllegalArgumentException if no properties are given.
  public static Transition of(CssDuration duration, Easing easing, Animatable... properties) {
    return new Transition(duration, easing, List.of(properties), false);
  }

  /// Control colors: background, text, border and outline color over
  /// [Tokens#CONTROL_FEEDBACK_DURATION] (150ms) with `ease`. Discrete values do not transition.
  public static Transition controlFeedback() {
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
  public static Transition overlayEntry() {
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
  public static Transition disclosure() {
    return of(
            Tokens.DISCLOSURE_DURATION,
            Easing.ease(),
            Animatable.BLOCK_SIZE,
            Animatable.CONTENT_VISIBILITY)
        .allowingDiscrete();
  }

  /// The same duration, easing and discrete setting over `replacement` properties. Throws
  /// IllegalArgumentException if none are given.
  public Transition properties(Animatable... replacement) {
    return new Transition(duration, easing, List.of(replacement), allowDiscrete);
  }

  /// Also transitions properties that switch between discrete values, such as `display`.
  public Transition allowingDiscrete() {
    return new Transition(duration, easing, properties, true);
  }

  /// Writes the transition declarations to `style`; [Styled#transition(Transition)] calls it.
  void writeTo(Declarations style) {
    style
        .put(CssProperty.TRANSITION_PROPERTY, properties)
        .put(CssProperty.TRANSITION_DURATION, duration)
        .put(CssProperty.TRANSITION_TIMING_FUNCTION, easing);
    if (allowDiscrete) {
      style.put(CssProperty.TRANSITION_BEHAVIOR, "allow-discrete");
    }
  }
}
