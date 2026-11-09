package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.Decimals;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/// A named `@keyframes` rule, built as a typed value: `Keyframes.named("pop").from(s ->
/// s.opacity(0)).to(s -> s.opacity(1))`. Play it with an [Animation]; any element or scope rule
/// that uses the animation registers the rule in the stylesheet, so there is nothing else to
/// declare. The value is immutable: [#from], [#to] and [#at] return a new one.
///
/// Each step keeps only the plain declarations its callback writes; conditional styles
/// (`on(...)`), starting styles and nested animations inside a step are ignored. The presets
/// ([#FADE_IN], [#FADE_UP], [#SCALE_IN], [#SLIDE_INLINE], [#FADE_OUT]) animate `opacity` and
/// `transform`. Give each definition its own name: two different rules with one name both emit,
/// and the later one wins.
public final class Keyframes {

  /// Fades from transparent to opaque.
  public static final Keyframes FADE_IN =
      named("tessera-fade-in").from(s -> s.opacity(0)).to(s -> s.opacity(1));

  /// Fades in while rising 1.25rem into place.
  public static final Keyframes FADE_UP =
      named("tessera-fade-up")
          .from(s -> s.opacity(0).transform(Transform.translateY(CssLength.rem(1.25))))
          .to(s -> s.opacity(1).transform(Transform.translateY(CssLength.zero())));

  /// Fades in while growing from 92% to full size.
  public static final Keyframes SCALE_IN =
      named("tessera-scale-in")
          .from(s -> s.opacity(0).transform(Transform.scale(0.92)))
          .to(s -> s.opacity(1).transform(Transform.scale(1)));

  /// Fades in while sliding 1.5rem in from the left.
  public static final Keyframes SLIDE_INLINE =
      named("tessera-slide-inline")
          .from(s -> s.opacity(0).transform(Transform.translateX(CssLength.rem(-1.5))))
          .to(s -> s.opacity(1).transform(Transform.translateX(CssLength.zero())));

  /// Fades out while shrinking to 95%.
  public static final Keyframes FADE_OUT =
      named("tessera-fade-out")
          .from(s -> s.opacity(1))
          .to(s -> s.opacity(0).transform(Transform.scale(0.95)));

  private final CssIdent ident;
  private final Map<Double, String> steps;

  private Keyframes(CssIdent ident, Map<Double, String> steps) {
    this.ident = ident;
    this.steps = steps;
  }

  /// Keyframes named `name`, with no steps yet. Throws IllegalArgumentException if `name` is not a
  /// CSS identifier, starts with `--`, or is `none`, `default` or a CSS-wide keyword.
  public static Keyframes named(String name) {
    return new Keyframes(CssIdent.of(name), Map.of());
  }

  /// The same keyframes with a `from` (0%) step written by `styles`, replacing any 0% step.
  public Keyframes from(Consumer<StyleRefinement> styles) {
    return at(0, styles);
  }

  /// The same keyframes with a `to` (100%) step written by `styles`, replacing any 100% step.
  public Keyframes to(Consumer<StyleRefinement> styles) {
    return at(100, styles);
  }

  /// The same keyframes with a step at `percent` of the animation written by `styles`, replacing
  /// any step at that offset. Throws IllegalArgumentException unless `percent` is within 0..100.
  public Keyframes at(double percent, Consumer<StyleRefinement> styles) {
    if (!(percent >= 0 && percent <= 100)) {
      throw new IllegalArgumentException("A keyframe offset is within 0..100: " + percent);
    }
    var style = new StyleRefinement();
    styles.accept(style);
    var next = new TreeMap<>(steps);
    next.put(percent, style.toCssBlockBody());
    return new Keyframes(ident, Map.copyOf(next));
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
    var sb = new StringBuilder("@keyframes ").append(ident.toCss()).append('{');
    new TreeMap<>(steps)
        .forEach(
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
    return rule();
  }
}
