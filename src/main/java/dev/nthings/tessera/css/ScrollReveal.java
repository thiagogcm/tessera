package dev.nthings.tessera.css;

/**
 * Compositor-friendly scroll-driven reveal and exit presets ({@code opacity}, {@code transform})
 * linked to {@code animation-timeline: view()}.
 */
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

  public String keyframeName() {
    return keyframeName;
  }

  public String keyframeRule() {
    return keyframeRule;
  }
}
