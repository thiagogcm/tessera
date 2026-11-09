package dev.nthings.tessera.css;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.foundation.Decimals;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/// A named `@keyframes` rule, built as a typed value: `Keyframes.named("pop").start(s ->
/// s.opacity(0)).end(s -> s.opacity(1))`. Play it with an [Animation]; any element or scope rule
/// that uses the animation registers the rule in the stylesheet, so there is nothing else to
/// declare. The value is immutable: [#start], [#end] and [#at] return a new one.
///
/// Each step keeps only the plain declarations its callback writes; conditional styles
/// (`on(...)`), starting styles and nested animations inside a step are ignored. The presets
/// ([#FADE_IN], [#FADE_UP], [#SCALE_IN], [#SLIDE_INLINE], [#FADE_OUT]) animate `opacity` and
/// `transform`. Give each definition its own name: two different rules with one name both emit,
/// and the later one wins.
@CheckReturnValue
public final class Keyframes {

  /// Fades from transparent to opaque.
  public static final Keyframes FADE_IN =
      named("tessera-fade-in").start(s -> s.opacity(0)).end(s -> s.opacity(1));

  /// Fades in while rising 1.25rem into place.
  public static final Keyframes FADE_UP =
      named("tessera-fade-up")
          .start(s -> s.opacity(0).transform(Transform.translateY(CssLength.rem(1.25))))
          .end(s -> s.opacity(1).transform(Transform.translateY(CssLength.zero())));

  /// Fades in while growing from 92% to full size.
  public static final Keyframes SCALE_IN =
      named("tessera-scale-in")
          .start(s -> s.opacity(0).transform(Transform.scale(0.92)))
          .end(s -> s.opacity(1).transform(Transform.scale(1)));

  /// Fades in while sliding 1.5rem in from the left.
  public static final Keyframes SLIDE_INLINE =
      named("tessera-slide-inline")
          .start(s -> s.opacity(0).transform(Transform.translateX(CssLength.rem(-1.5))))
          .end(s -> s.opacity(1).transform(Transform.translateX(CssLength.zero())));

  /// Fades out while shrinking to 95%.
  public static final Keyframes FADE_OUT =
      named("tessera-fade-out")
          .start(s -> s.opacity(1))
          .end(s -> s.opacity(0).transform(Transform.scale(0.95)));

  private final CssIdent ident;

  /// Step bodies by offset, ascending.
  private final SortedMap<Double, String> steps;

  private final String rule;

  private Keyframes(CssIdent ident, SortedMap<Double, String> steps) {
    this.ident = ident;
    this.steps = Collections.unmodifiableSortedMap(steps);
    this.rule = rule(ident, steps);
  }

  /// Keyframes named `name`, with no steps yet. Throws IllegalArgumentException if `name` is not a
  /// CSS identifier, starts with `--`, or is `none`, `default` or a CSS-wide keyword.
  public static Keyframes named(String name) {
    return new Keyframes(CssIdent.of(name), new TreeMap<>());
  }

  /// The same keyframes with a start step (0%, emitted as `from`) written by `styles`, replacing
  /// any 0% step.
  public Keyframes start(StyleBlock styles) {
    return at(0, styles);
  }

  /// The same keyframes with an end step (100%, emitted as `to`) written by `styles`, replacing
  /// any 100% step.
  public Keyframes end(StyleBlock styles) {
    return at(100, styles);
  }

  /// The same keyframes with a step at `percent` of the animation written by `styles`, replacing
  /// any step at that offset. Throws IllegalArgumentException unless `percent` is within 0..100.
  public Keyframes at(double percent, StyleBlock styles) {
    if (!(percent >= 0 && percent <= 100)) {
      throw new IllegalArgumentException("A keyframe offset is within 0..100: " + percent);
    }
    var style = new Declarations();
    styles.configure(style);
    var next = new TreeMap<>(steps);
    next.put(percent, style.toCssBlockBody());
    return new Keyframes(ident, next);
  }

  /// The animation name.
  String name() {
    return ident.toCss();
  }

  /// Whether no step has been added.
  boolean isEmpty() {
    return steps.isEmpty();
  }

  /// The complete `@keyframes` rule.
  String rule() {
    return rule;
  }

  private static String rule(CssIdent ident, SortedMap<Double, String> steps) {
    var sb = new StringBuilder("@keyframes ").append(ident.toCss()).append('{');
    steps.forEach(
        (offset, body) -> sb.append(selector(offset)).append('{').append(body).append('}'));
    return sb.append('}').toString();
  }

  private static String selector(double offset) {
    if (offset == 0) {
      return "from";
    }
    if (offset == 100) {
      return "to";
    }
    return Decimals.format(offset) + "%";
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof Keyframes keyframes
        && keyframes.ident.equals(ident)
        && keyframes.steps.equals(steps);
  }

  @Override
  public int hashCode() {
    return 31 * ident.hashCode() + steps.hashCode();
  }

  @Override
  public String toString() {
    return rule;
  }
}
