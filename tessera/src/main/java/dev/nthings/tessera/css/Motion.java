package dev.nthings.tessera.css;

/// Duration, easing, and transition properties for one purpose.
///
/// Spatial presets belong inside `prefers-reduced-motion: no-preference`. The visible end state
/// stays outside that query, so the control is already in place when the transition does not run.
public record Motion(
    DesignToken.Duration duration, String easing, String properties, boolean discrete) {

  public static Motion controlFeedback() {
    return new Motion(
        Tokens.CONTROL_FEEDBACK_DURATION,
        "ease",
        "background, color, border-color, outline-color",
        false);
  }

  public static Motion overlayEntry() {
    return new Motion(
        Tokens.OVERLAY_DURATION,
        "ease-out",
        "opacity, transform, translate, display, overlay",
        true);
  }

  public static Motion disclosure() {
    return new Motion(Tokens.DISCLOSURE_DURATION, "ease", "block-size, content-visibility", true);
  }

  public Motion properties(String properties) {
    return new Motion(duration, easing, properties, discrete);
  }

  public StyleRefinement apply(StyleRefinement style) {
    style
        .put(CssProperty.TRANSITION_PROPERTY, properties)
        .put(CssProperty.TRANSITION_DURATION, duration.varReference())
        .put(CssProperty.TRANSITION_TIMING_FUNCTION, easing);
    if (discrete) {
      style.put(CssProperty.TRANSITION_BEHAVIOR, "allow-discrete");
    }
    return style;
  }
}
