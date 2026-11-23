package dev.nthings.tessera.css;

import com.google.errorprone.annotations.CheckReturnValue;
import java.util.List;

/// A transition: the properties it animates, with a duration, an easing, whether discrete values
/// such as `display` transition too, and whether it runs only when the user has not asked for
/// reduced motion. Applied with [Styled#transition(Transition)].
///
/// A transition that moves an element should run only under [Media#MOTION_SAFE]: build it with
/// [#whenMotionSafe()] (spring transitions are built that way), or write it inside
/// `on(Media.MOTION_SAFE, ...)`. Keep the visible end state outside it, so the element is in place
/// even when no transition runs.
///
/// @param motionSafe whether the transition is written under [Media#MOTION_SAFE], so it runs only
///   when the user has not asked for reduced motion
@CheckReturnValue
public record Transition(
    CssDuration duration,
    Easing easing,
    List<Animatable> properties,
    boolean allowDiscrete,
    boolean motionSafe) {

  public Transition {
    if (properties.isEmpty()) {
      throw new IllegalArgumentException("A transition animates at least one property");
    }
    properties = List.copyOf(properties);
  }

  /// Transitions `properties` over `duration` with `easing`, whatever the user's motion preference.
  /// Discrete values do not transition. Throws IllegalArgumentException if no properties are given.
  public static Transition of(CssDuration duration, Easing easing, Animatable... properties) {
    return new Transition(duration, easing, List.of(properties), false, false);
  }

  /// Transitions `properties` along `spring`: over its [Spring#settlingDuration()] with its
  /// compiled [Spring#easing()]. A spring is motion, so the transition runs only under
  /// [Media#MOTION_SAFE]; with reduced motion the properties change at once, or follow a plain
  /// transition written outside it, such as a short opacity fade. A bouncy spring overshoots, so it
  /// suits position, size and transforms; opacity and colors clamp at the ends of their range.
  /// Throws IllegalArgumentException if no properties are given.
  ///
  /// To emit a spring's curve once in the theme rather than in every rule, put it in a
  /// [DesignToken.Easing] and [DesignToken.Duration] and use them with [#of] and
  /// [#whenMotionSafe()].
  public static Transition spring(Spring spring, Animatable... properties) {
    return new Transition(
        spring.settlingDuration(), spring.easing(), List.of(properties), false, true);
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

  /// Disclosure: block size and `content-visibility` over [Tokens#DISCLOSURE_DURATION] (250ms) with
  /// `ease`. Discrete values transition, so hidden content can animate open.
  public static Transition disclosure() {
    return of(
            Tokens.DISCLOSURE_DURATION,
            Easing.ease(),
            Animatable.BLOCK_SIZE,
            Animatable.CONTENT_VISIBILITY)
        .allowingDiscrete();
  }

  /// The same duration, easing, discrete and motion settings over `replacement` properties. Throws
  /// IllegalArgumentException if none are given.
  public Transition properties(Animatable... replacement) {
    return new Transition(duration, easing, List.of(replacement), allowDiscrete, motionSafe);
  }

  /// Also transitions properties that switch between discrete values, such as `display`.
  public Transition allowingDiscrete() {
    return new Transition(duration, easing, properties, true, motionSafe);
  }

  /// The same transition, run only when the user has not asked for reduced motion: it is written
  /// under [Media#MOTION_SAFE], so with reduced motion the properties change at once.
  public Transition whenMotionSafe() {
    return new Transition(duration, easing, properties, allowDiscrete, true);
  }

  /// Writes the transition declarations to `style`, under [Media#MOTION_SAFE] when the transition
  /// is motion-safe; [Styled#transition(Transition)] calls it.
  void writeTo(Declarations style) {
    var target = motionSafe ? style.nested(Media.MOTION_SAFE) : style;
    target
        .put(CssProperty.TRANSITION_PROPERTY, properties)
        .put(CssProperty.TRANSITION_DURATION, duration)
        .put(CssProperty.TRANSITION_TIMING_FUNCTION, easing);
    if (allowDiscrete) {
      target.put(CssProperty.TRANSITION_BEHAVIOR, "allow-discrete");
    }
  }
}
