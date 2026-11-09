package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

/// The conditional styles of one element: pseudo-class states, group hover, reduced-motion rules,
/// container-width rules, and the `@keyframes` they use. Authors set them through the element
/// methods (`hover`, `motionSafe`, `onContainer`, `scrollReveal`, ...) rather than by calling this
/// class; [StyleSheet] compiles them.
public final class Interactivity {

  /// A rule whose `style` applies while the `containerName` container is at least
  /// `minInlineSize` wide.
  record ContainerRule(CssIdent containerName, CssLength minInlineSize, StyleRefinement style) {

    /// The `@container` condition, such as `(inline-size >= 20rem)`.
    String condition() {
      return "(inline-size >= " + minInlineSize.toCss() + ")";
    }

    /// A content hash that names the generated container rule.
    long contentHash() {
      var h = Fnv1a64.OFFSET_BASIS;
      h = Fnv1a64.updateChars(h, containerName.toCss());
      h = Fnv1a64.updateChars(h, condition());
      return Fnv1a64.updateLong(h, style.contentHash());
    }
  }

  /// The key of one nested rule. Sorted by state, then argument, so emission is deterministic.
  record StateRule(InteractiveState state, String argument) implements Comparable<StateRule> {
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

  /// Runs `styles` on the declarations for `state`. Calling again for the same state extends the
  /// same declarations.
  public void on(InteractiveState state, Consumer<StyleRefinement> styles) {
    on(state, "", styles);
  }

  /// Runs `styles` on the declarations for `state` and `argument`: the sanitized group name for
  /// `GROUP_HOVER`, and empty for the other states.
  public void on(InteractiveState state, String argument, Consumer<StyleRefinement> styles) {
    styles.accept(
        states.computeIfAbsent(new StateRule(state, argument), _ -> new StyleRefinement()));
  }

  /// Registers a complete `@keyframes` rule, emitted once in `@layer base`. The text is trusted raw
  /// CSS and is not validated.
  public void registerKeyframeRule(String keyframeRule) {
    keyframes.add(keyframeRule);
  }

  /// Runs `styles` on declarations that apply while the `containerName` container is at least
  /// `minInlineSize` wide.
  public void onContainerMinWidth(
      CssIdent containerName, CssLength minInlineSize, Consumer<StyleRefinement> styles) {
    var style = new StyleRefinement();
    styles.accept(style);
    containerRules.add(new ContainerRule(containerName, minInlineSize, style));
  }

  /// Plays `preset` as the element scrolls into view along `axis`, registering its `@keyframes`.
  /// Applies only under `prefers-reduced-motion: no-preference`.
  public void scrollReveal(ScrollReveal preset, TimelineAxis axis) {
    scrollDriven(preset, axis, false);
  }

  /// Plays `preset` as the element scrolls out of view along `axis`, keeping its end state and
  /// registering its `@keyframes`. Applies only under `prefers-reduced-motion: no-preference`.
  public void scrollExit(ScrollReveal preset, TimelineAxis axis) {
    scrollDriven(preset, axis, true);
  }

  private void scrollDriven(ScrollReveal preset, TimelineAxis axis, boolean exit) {
    registerKeyframeRule(preset.keyframeRule());
    on(InteractiveState.MOTION_SAFE, style -> preset.scrollDriven(style, axis, exit));
  }

  /// Plays `preset` once, over 0.5s, the first time the element scrolls into view, registering its
  /// `@keyframes`. Applies only under `prefers-reduced-motion: no-preference`.
  public void revealOnce(ScrollReveal preset) {
    registerKeyframeRule(preset.keyframeRule());
    on(InteractiveState.MOTION_SAFE, preset::revealOnce);
  }

  /// Merges `other` into this one, copying refinements so the two stay independent. Merged state
  /// declarations follow the last-call-wins rule of [StyleRefinement#mergeFrom].
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

  /// The nested rules, in emission order.
  SortedMap<StateRule, StyleRefinement> states() {
    return states;
  }

  /// The raw `@keyframes` rules to emit.
  Set<String> keyframes() {
    return keyframes;
  }

  /// The container-width rules, in registration order.
  List<ContainerRule> containerRules() {
    return containerRules;
  }

  /// Whether any state rule declares something.
  boolean hasInteractiveStyles() {
    return states.values().stream().anyMatch(style -> !style.isEmpty());
  }
}
