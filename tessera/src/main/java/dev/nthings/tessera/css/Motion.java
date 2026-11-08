package dev.nthings.tessera.css;

import java.util.List;

/// Spatial presets belong inside `prefers-reduced-motion: no-preference`. Keep the visible end
/// state outside it, so the control is in place even when no transition runs.
public record Motion(
    CssDuration duration, Easing easing, List<Animatable> properties, boolean allowDiscrete) {

  public Motion {
    if (properties.isEmpty()) {
      throw new IllegalArgumentException("A motion transitions at least one property");
    }
    properties = List.copyOf(properties);
  }

  public static Motion of(CssDuration duration, Easing easing, Animatable... properties) {
    return new Motion(duration, easing, List.of(properties), false);
  }

  public static Motion controlFeedback() {
    return of(
        Tokens.CONTROL_FEEDBACK_DURATION,
        Easing.ease(),
        Animatable.BACKGROUND,
        Animatable.COLOR,
        Animatable.BORDER_COLOR,
        Animatable.OUTLINE_COLOR);
  }

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

  public static Motion disclosure() {
    return of(
            Tokens.DISCLOSURE_DURATION,
            Easing.ease(),
            Animatable.BLOCK_SIZE,
            Animatable.CONTENT_VISIBILITY)
        .allowingDiscrete();
  }

  /// The same timing over different properties.
  public Motion properties(Animatable... replacement) {
    return new Motion(duration, easing, List.of(replacement), allowDiscrete);
  }

  /// Also transitions properties that switch between discrete values.
  public Motion allowingDiscrete() {
    return new Motion(duration, easing, properties, true);
  }

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
