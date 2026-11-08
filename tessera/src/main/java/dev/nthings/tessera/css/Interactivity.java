package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

/// Interactive state rules for one element: pseudo-classes, group hover, `:has()`, and
/// container-width rules. Authors set them through the `InteractiveElement` methods (`hover`,
/// `motionSafe`, `whenHas`, `onContainer`, ...) rather than by calling this class.
public final class Interactivity {

  /// A rule whose `style` applies while the `containerName` container is at least
  /// `minInlineSize` wide.
  public record ContainerRule(
      CssIdent containerName, CssLength minInlineSize, StyleRefinement style) {

    /// Compiler-facing: the `@container` condition, such as `(inline-size >= 20rem)`.
    public String condition() {
      return "(inline-size >= " + minInlineSize.toCss() + ")";
    }

    /// Compiler-facing: a content hash that names the generated container rule.
    public long contentHash() {
      var h = Fnv1a64.OFFSET_BASIS;
      h = Fnv1a64.updateChars(h, containerName.toCss());
      h = Fnv1a64.updateChars(h, condition());
      return Fnv1a64.updateLong(h, style.contentHash());
    }
  }

  /// Compiler-facing: the key of one nested rule. Sorted by state, then argument, so emission is
  /// deterministic.
  public record StateRule(InteractiveState state, String argument)
      implements Comparable<StateRule> {
    private static final Comparator<StateRule> ORDER =
        Comparator.comparing(StateRule::state).thenComparing(StateRule::argument);

    @Override
    public int compareTo(StateRule other) {
      return ORDER.compare(this, other);
    }

    String openRule() {
      return state.openRule(argument);
    }
  }

  private final SortedMap<StateRule, StyleRefinement> states = new TreeMap<>();
  private final Set<String> keyframes = new LinkedHashSet<>();
  private final List<ContainerRule> containerRules = new ArrayList<>();

  /// Applies `mutator` to the declarations for `state`.
  public void on(InteractiveState state, UnaryOperator<StyleRefinement> mutator) {
    on(state, "", mutator);
  }

  /// Applies `mutator` to the declarations for `state` and `argument`: the group name for
  /// `GROUP_HOVER`, the selector for `HAS`, and empty for the other states.
  public void on(InteractiveState state, String argument, UnaryOperator<StyleRefinement> mutator) {
    mutator.apply(
        states.computeIfAbsent(new StateRule(state, argument), _ -> new StyleRefinement()));
  }

  /// Registers a complete `@keyframes` rule, emitted once in `@layer base`. The text is trusted raw
  /// CSS and is not validated.
  public void registerKeyframeRule(String keyframeRule) {
    keyframes.add(keyframeRule);
  }

  /// Applies `mutator` while the `containerName` container is at least `minInlineSize` wide.
  public void onContainerMinWidth(
      CssIdent containerName, CssLength minInlineSize, UnaryOperator<StyleRefinement> mutator) {
    var style = mutator.apply(new StyleRefinement());
    containerRules.add(new ContainerRule(containerName, minInlineSize, style));
  }

  /// Merges `other` into this one, copying refinements so the two stay independent.
  public Interactivity mergeFrom(Interactivity other) {
    other.states.forEach(
        (rule, style) -> states.computeIfAbsent(rule, _ -> new StyleRefinement()).mergeFrom(style));
    keyframes.addAll(other.keyframes);
    other.containerRules.forEach(
        rule ->
            containerRules.add(
                new ContainerRule(
                    rule.containerName(),
                    rule.minInlineSize(),
                    new StyleRefinement().mergeFrom(rule.style()))));
    return this;
  }

  /// Compiler-facing: the nested rules, in emission order.
  public SortedMap<StateRule, StyleRefinement> states() {
    return Collections.unmodifiableSortedMap(states);
  }

  /// Compiler-facing: the raw `@keyframes` rules to emit.
  public Set<String> keyframes() {
    return Collections.unmodifiableSet(keyframes);
  }

  /// Compiler-facing: the container-width rules, in registration order.
  public List<ContainerRule> containerRules() {
    return Collections.unmodifiableList(containerRules);
  }

  /// Compiler-facing: whether any state rule declares something.
  public boolean hasInteractiveStyles() {
    return states.values().stream().anyMatch(style -> !style.isEmpty());
  }
}
