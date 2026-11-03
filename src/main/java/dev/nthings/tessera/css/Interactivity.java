package dev.nthings.tessera.css;

import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Fnv1a64;
import dev.nthings.tessera.core.TagElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/**
 * Stores pseudo-class, group-hover, relational {@code :has()}, {@code @container} queries, and
 * declarative invoker metadata for an element.
 */
public final class Interactivity {

  public record ContainerRule(String containerName, String condition, StyleRefinement style) {
    public long contentHash() {
      long h = Fnv1a64.OFFSET_BASIS;
      h = Fnv1a64.updateChars(h, containerName);
      h = Fnv1a64.updateChars(h, condition);
      return Fnv1a64.updateLong(h, style.contentHash());
    }
  }

  private @Nullable StyleRefinement hoverStyle;
  private @Nullable StyleRefinement focusVisibleStyle;
  private @Nullable StyleRefinement activeStyle;
  private final Map<String, StyleRefinement> groupHoverStyles = new LinkedHashMap<>();
  private final Map<String, StyleRefinement> hasStyles = new LinkedHashMap<>();
  private final List<ContainerRule> containerRules = new ArrayList<>();

  private @Nullable ElementId commandTargetId;
  private @Nullable String commandAction;
  private @Nullable TagElement interestTooltip;

  public void onHover(UnaryOperator<StyleRefinement> mutator) {
    if (hoverStyle == null) {
      hoverStyle = new StyleRefinement();
    }
    mutator.apply(hoverStyle);
  }

  public void onFocusVisible(UnaryOperator<StyleRefinement> mutator) {
    if (focusVisibleStyle == null) {
      focusVisibleStyle = new StyleRefinement();
    }
    mutator.apply(focusVisibleStyle);
  }

  public void onActive(UnaryOperator<StyleRefinement> mutator) {
    if (activeStyle == null) {
      activeStyle = new StyleRefinement();
    }
    mutator.apply(activeStyle);
  }

  public void onGroupHover(String groupName, UnaryOperator<StyleRefinement> mutator) {
    var refinement = groupHoverStyles.computeIfAbsent(groupName, _ -> new StyleRefinement());
    mutator.apply(refinement);
  }

  public void onHas(String relativeSelector, UnaryOperator<StyleRefinement> mutator) {
    var refinement = hasStyles.computeIfAbsent(relativeSelector, _ -> new StyleRefinement());
    mutator.apply(refinement);
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

  public void setCommandTarget(ElementId targetId, String command) {
    this.commandTargetId = targetId;
    this.commandAction = command;
  }

  public void setInterestTooltip(TagElement tooltip) {
    this.interestTooltip = tooltip;
  }

  public @Nullable StyleRefinement hoverStyle() {
    return hoverStyle;
  }

  public @Nullable StyleRefinement focusVisibleStyle() {
    return focusVisibleStyle;
  }

  public @Nullable StyleRefinement activeStyle() {
    return activeStyle;
  }

  public Map<String, StyleRefinement> groupHoverStyles() {
    return Collections.unmodifiableMap(groupHoverStyles);
  }

  public Map<String, StyleRefinement> hasStyles() {
    return Collections.unmodifiableMap(hasStyles);
  }

  public List<ContainerRule> containerRules() {
    return Collections.unmodifiableList(containerRules);
  }

  public @Nullable ElementId commandTargetId() {
    return commandTargetId;
  }

  public @Nullable String commandAction() {
    return commandAction;
  }

  public @Nullable TagElement interestTooltip() {
    return interestTooltip;
  }

  public boolean hasInteractiveStyles() {
    return (hoverStyle != null && !hoverStyle.isEmpty())
        || (focusVisibleStyle != null && !focusVisibleStyle.isEmpty())
        || (activeStyle != null && !activeStyle.isEmpty())
        || !groupHoverStyles.isEmpty()
        || !hasStyles.isEmpty();
  }

  public boolean hasContainerRules() {
    return !containerRules.isEmpty();
  }
}
