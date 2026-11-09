package dev.nthings.tessera.css;

/// Reveal and exit presets that animate `opacity` and `transform`. The element methods
/// `scrollReveal` and `scrollExit` tie the animation to scroll progress with
/// `animation-timeline: view()`, while `revealOnce` plays it for 0.5s the first time the element
/// scrolls into view. Each registers the preset's `@keyframes` rule and applies only under
/// `prefers-reduced-motion: no-preference`.
public enum ScrollReveal {
  FADE_IN("tessera-fade-in", "@keyframes tessera-fade-in{from{opacity:0}to{opacity:1}}"),
  FADE_UP(
      "tessera-fade-up",
      "@keyframes tessera-fade-up{from{opacity:0;transform:translateY(1.25rem)}to{opacity:1;transform:translateY(0)}}"),
  SCALE_IN(
      "tessera-scale-in",
      "@keyframes tessera-scale-in{from{opacity:0;transform:scale(0.92)}to{opacity:1;transform:scale(1)}}"),
  SLIDE_INLINE(
      "tessera-slide-inline",
      "@keyframes tessera-slide-inline{from{opacity:0;transform:translateX(-1.5rem)}to{opacity:1;transform:translateX(0)}}"),
  FADE_OUT(
      "tessera-fade-out",
      "@keyframes tessera-fade-out{from{opacity:1}to{opacity:0;transform:scale(0.95)}}");

  private final String keyframeName;
  private final String keyframeRule;

  ScrollReveal(String keyframeName, String keyframeRule) {
    this.keyframeName = keyframeName;
    this.keyframeRule = keyframeRule;
  }

  /// The `@keyframes` rule text, registered by the [Interactivity] methods that apply a preset.
  String keyframeRule() {
    return keyframeRule;
  }

  /// Drives the preset by progress through the viewport on `axis`: the entry range, or the exit
  /// range when `exit` is set. Entry holds the start keyframe before the element arrives, and exit
  /// holds the end keyframe after it leaves. Does not register the `@keyframes` rule.
  StyleRefinement scrollDriven(StyleRefinement style, TimelineAxis axis, boolean exit) {
    var phase = exit ? "exit" : "entry";
    return style
        .put(
            CssProperty.ANIMATION,
            keyframeName + " auto linear " + (exit ? "forwards" : "backwards"))
        .put(CssProperty.ANIMATION_TIMELINE, "view(" + axis.toCss() + ")")
        .put(CssProperty.ANIMATION_RANGE, phase + " 0% " + phase + " 100%");
  }

  /// Plays the preset once, for 0.5s with `ease-out`, the first time any part of the element
  /// scrolls into view. It is time-based, so it does not follow the scroll position. Does not
  /// register the `@keyframes` rule.
  StyleRefinement revealOnce(StyleRefinement style) {
    return style
        .put(CssProperty.ANIMATION, keyframeName + " 0.5s ease-out both")
        .put(CssProperty.TIMELINE_TRIGGER, "--tessera-reveal view() cover 0% cover 100%")
        .put(CssProperty.ANIMATION_TRIGGER, "--tessera-reveal play-once");
  }
}
