package dev.nthings.tessera.css;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/// Plays [Keyframes] on an element, applied with [Styled#animation(Animation)]. Every animation
/// applies only under `prefers-reduced-motion: no-preference` ([Media#MOTION_SAFE]), and using
/// one registers its keyframes in the stylesheet. The value is immutable: [#along],
/// [#staggerBySiblingIndex] and the other modifiers return a new one.
///
/// Timelines:
///
/// - [#of]: plays once over a duration when the style applies.
/// - [#reveal]: follows the element's progress into the viewport, so scrolling back reverses it.
/// - [#exit]: follows the element's progress out of the viewport, and keeps the end state.
/// - [#revealOnce]: plays once, over 0.5s, the first time the element scrolls into view.
/// - [#scrollProgress]: follows the scroll position of the whole page.
public final class Animation {

  private enum Timeline {
    TIME,
    VIEW_ENTRY,
    VIEW_EXIT,
    ONCE_IN_VIEW,
    ROOT_SCROLL
  }

  private final Keyframes keyframes;
  private final Timeline timeline;
  private final @Nullable CssDuration duration;
  private final @Nullable Easing easing;
  private final TimelineAxis axis;
  private final int staggerMilliseconds;

  private Animation(
      Keyframes keyframes,
      Timeline timeline,
      @Nullable CssDuration duration,
      @Nullable Easing easing,
      TimelineAxis axis,
      int staggerMilliseconds) {
    if (keyframes.isEmpty()) {
      throw new IllegalArgumentException("Keyframes '" + keyframes.name() + "' have no steps");
    }
    this.keyframes = keyframes;
    this.timeline = timeline;
    this.duration = duration;
    this.easing = easing;
    this.axis = axis;
    this.staggerMilliseconds = staggerMilliseconds;
  }

  private static Animation on(Keyframes keyframes, Timeline timeline) {
    return new Animation(keyframes, timeline, null, null, TimelineAxis.BLOCK, -1);
  }

  /// Plays `keyframes` once over `duration` with `easing`, starting when the style applies. Throws
  /// IllegalArgumentException if `keyframes` has no steps.
  public static Animation of(Keyframes keyframes, CssDuration duration, Easing easing) {
    return new Animation(keyframes, Timeline.TIME, duration, easing, TimelineAxis.BLOCK, -1);
  }

  /// Drives `keyframes` by the element's progress into the viewport along the block axis
  /// (`animation-timeline: view()`, entry range). Before the element arrives it holds the first
  /// keyframe, and scrolling back reverses the animation. Throws IllegalArgumentException if
  /// `keyframes` has no steps.
  public static Animation reveal(Keyframes keyframes) {
    return on(keyframes, Timeline.VIEW_ENTRY);
  }

  /// Drives `keyframes` by the element's progress out of the viewport along the block axis (exit
  /// range), holding the last keyframe after it leaves. Throws IllegalArgumentException if
  /// `keyframes` has no steps.
  public static Animation exit(Keyframes keyframes) {
    return on(keyframes, Timeline.VIEW_EXIT);
  }

  /// Plays `keyframes` once, over 0.5s with `ease-out`, the first time any part of the element
  /// scrolls into view (a CSS scroll trigger; an element visible on load plays immediately). It is
  /// time-based, so it does not follow or reverse with the scroll position. Throws
  /// IllegalArgumentException if `keyframes` has no steps.
  public static Animation revealOnce(Keyframes keyframes) {
    return on(keyframes, Timeline.ONCE_IN_VIEW);
  }

  /// Drives `keyframes` by the scroll position of the page along the block axis
  /// (`animation-timeline: scroll(root)`), from the top to the bottom. Throws
  /// IllegalArgumentException if `keyframes` has no steps.
  public static Animation scrollProgress(Keyframes keyframes) {
    return on(keyframes, Timeline.ROOT_SCROLL);
  }

  /// The same animation tracking `timelineAxis` instead of the block axis. Throws
  /// IllegalStateException for a time-based animation from [#of], which has no scroll axis.
  public Animation along(TimelineAxis timelineAxis) {
    if (timeline == Timeline.TIME) {
      throw new IllegalStateException("A time-based animation has no scroll axis");
    }
    return new Animation(keyframes, timeline, duration, easing, timelineAxis, staggerMilliseconds);
  }

  /// The same animation delayed by `stepMilliseconds` times the element's position among its
  /// siblings, so the first sibling starts at once and later ones cascade. Throws
  /// IllegalArgumentException if `stepMilliseconds` is negative.
  public Animation staggerBySiblingIndex(int stepMilliseconds) {
    if (stepMilliseconds < 0) {
      throw new IllegalArgumentException("stepMilliseconds must be >= 0");
    }
    return new Animation(keyframes, timeline, duration, easing, axis, stepMilliseconds);
  }

  Keyframes keyframes() {
    return keyframes;
  }

  /// Writes the animation declarations to `style`, without the reduced-motion guard.
  void apply(StyleRefinement style) {
    var name = keyframes.name();
    switch (timeline) {
      case TIME -> {
        var time = Objects.requireNonNull(duration).toCss();
        style.put(
            CssProperty.ANIMATION,
            name + " " + time + " " + Objects.requireNonNull(easing).toCss());
      }
      case VIEW_ENTRY -> viewRange(style, name + " auto linear backwards", "entry");
      case VIEW_EXIT -> viewRange(style, name + " auto linear forwards", "exit");
      case ONCE_IN_VIEW -> {
        var view = axis == TimelineAxis.BLOCK ? "view()" : "view(" + axis.toCss() + ")";
        style
            .put(CssProperty.ANIMATION, name + " 0.5s ease-out both")
            .put(CssProperty.TIMELINE_TRIGGER, "--tessera-reveal " + view + " cover 0% cover 100%")
            .put(CssProperty.ANIMATION_TRIGGER, "--tessera-reveal play-once");
      }
      case ROOT_SCROLL ->
          style
              .put(CssProperty.ANIMATION, name + " auto linear")
              .put(CssProperty.ANIMATION_TIMELINE, "scroll(root " + axis.toCss() + ")");
    }
    if (staggerMilliseconds >= 0) {
      style.put(
          CssProperty.ANIMATION_DELAY,
          "calc((sibling-index() - 1) * " + staggerMilliseconds + "ms)");
    }
  }

  private void viewRange(StyleRefinement style, String shorthand, String phase) {
    style
        .put(CssProperty.ANIMATION, shorthand)
        .put(CssProperty.ANIMATION_TIMELINE, "view(" + axis.toCss() + ")")
        .put(CssProperty.ANIMATION_RANGE, phase + " 0% " + phase + " 100%");
  }
}
