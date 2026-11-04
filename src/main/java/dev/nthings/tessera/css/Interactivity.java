package dev.nthings.tessera.css;

import dev.nthings.tessera.core.Fnv1a64;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

/// Stores pseudo-class, group-hover, relational `:has()`, `@container` query, and scroll-driven
/// animation styles for an element.
public final class Interactivity {

  public record ContainerRule(String containerName, String condition, StyleRefinement style) {
    public long contentHash() {
      long h = Fnv1a64.OFFSET_BASIS;
      h = Fnv1a64.updateChars(h, containerName);
      h = Fnv1a64.updateChars(h, condition);
      return Fnv1a64.updateLong(h, style.contentHash());
    }
  }

  /// Interactive state rule key; ordered by state, then argument, for deterministic emission.
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

  public void on(InteractiveState state, UnaryOperator<StyleRefinement> mutator) {
    on(state, "", mutator);
  }

  public void on(InteractiveState state, String argument, UnaryOperator<StyleRefinement> mutator) {
    mutator.apply(
        states.computeIfAbsent(new StateRule(state, argument), _ -> new StyleRefinement()));
  }

  public void registerKeyframeRule(String keyframeRule) {
    keyframes.add(keyframeRule);
  }

  public void onContainerMinWidth(
      String containerName, CssLength minInlineSize, UnaryOperator<StyleRefinement> mutator) {
    var style = mutator.apply(new StyleRefinement());
    containerRules.add(
        new ContainerRule(containerName, "(inline-size >= " + minInlineSize.toCss() + ")", style));
  }

  public void onContainerStyleQuery(
      String containerName,
      String customProperty,
      String expectedValue,
      UnaryOperator<StyleRefinement> mutator) {
    var style = mutator.apply(new StyleRefinement());
    containerRules.add(
        new ContainerRule(
            containerName, "style(" + customProperty + ": " + expectedValue + ")", style));
  }

  public SortedMap<StateRule, StyleRefinement> states() {
    return Collections.unmodifiableSortedMap(states);
  }

  public Set<String> keyframes() {
    return Collections.unmodifiableSet(keyframes);
  }

  public List<ContainerRule> containerRules() {
    return Collections.unmodifiableList(containerRules);
  }

  public boolean hasInteractiveStyles() {
    return states.values().stream().anyMatch(style -> !style.isEmpty());
  }
}
