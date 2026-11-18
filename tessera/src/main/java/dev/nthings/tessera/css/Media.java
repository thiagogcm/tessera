package dev.nthings.tessera.css;

/// A media feature for [Styled#on(Condition, StyleBlock)] and [ScopeStyle#on(Condition.Query,
/// ScopeBlock)], emitted as an `@media` rule.
public enum Media implements Condition.Query {
  /// `prefers-reduced-motion: no-preference`: the user has not asked for less motion. Put movement
  /// here and keep the visible end state outside, so the element is in place without the motion.
  MOTION_SAFE("@media (prefers-reduced-motion: no-preference)"),
  /// `forced-colors: active`: the browser replaces colors with the user's palette, such as Windows
  /// contrast themes. Backgrounds become the canvas color and background images disappear, so put
  /// state shown only through them here, drawn with borders, outlines or native rendering.
  FORCED_COLORS("@media (forced-colors: active)");

  private final String prelude;

  Media(String prelude) {
    this.prelude = prelude;
  }

  /// The `@media` prelude that opens the nested rule.
  String prelude() {
    return prelude;
  }
}
