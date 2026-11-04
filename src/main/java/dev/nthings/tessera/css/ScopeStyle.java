package dev.nthings.tessera.css;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/// Type-safe builder for `@scope ([data-view="..."]) to ([data-view])` rules in `@layer
/// components`.
///
/// Eliminates raw CSS strings inside [dev.nthings.tessera.core.RenderView] implementations by
/// expressing `:scope`, descendant selectors, `@starting-style` entry animations, `@container` size
/// and `scroll-state(...)` queries, and `@media (prefers-reduced-motion: no-preference)` blocks
/// through [StyleRefinement].
public final class ScopeStyle {

  private final Map<String, StyleRefinement> selectorRules = new LinkedHashMap<>();
  private final Map<String, StyleRefinement> startingStyleRules = new LinkedHashMap<>();
  private final Map<String, ScopeStyle> atRuleBlocks = new LinkedHashMap<>();

  public ScopeStyle root(UnaryOperator<StyleRefinement> mutator) {
    return select(":scope", mutator);
  }

  public ScopeStyle select(String selector, UnaryOperator<StyleRefinement> mutator) {
    var refinement = selectorRules.computeIfAbsent(selector, _ -> new StyleRefinement());
    mutator.apply(refinement);
    return this;
  }

  /// Defines an open/active selector alongside its nested `@starting-style` entry transition
  /// block.
  public ScopeStyle selectWithStartingStyle(
      String selector,
      UnaryOperator<StyleRefinement> openMutator,
      UnaryOperator<StyleRefinement> startingMutator) {
    select(selector, openMutator);
    var starting = startingStyleRules.computeIfAbsent(selector, _ -> new StyleRefinement());
    startingMutator.apply(starting);
    return this;
  }

  public ScopeStyle containerMinInlineSize(
      CssLength minInlineSize, Consumer<ScopeStyle> nestedBuilder) {
    return container("(inline-size >= " + minInlineSize.toCss() + ")", nestedBuilder);
  }

  public ScopeStyle containerMaxInlineSize(
      CssLength maxInlineSize, Consumer<ScopeStyle> nestedBuilder) {
    return container("(inline-size < " + maxInlineSize.toCss() + ")", nestedBuilder);
  }

  /// Emits a `@container [<name>] scroll-state(<stateQuery>)` block (e.g., `stuck: top`,
  /// `snapped: inline`, `scrollable: block`).
  public ScopeStyle containerScrollState(
      @Nullable String containerName, String stateQuery, Consumer<ScopeStyle> nestedBuilder) {
    String prefix =
        (containerName == null || containerName.isBlank()) ? "" : containerName.trim() + " ";
    return container(prefix + "scroll-state(" + stateQuery + ")", nestedBuilder);
  }

  public ScopeStyle container(String condition, Consumer<ScopeStyle> nestedBuilder) {
    return atRule("@container " + condition, nestedBuilder);
  }

  /// Wraps nested rules inside `@media (prefers-reduced-motion: no-preference)`.
  public ScopeStyle motionSafe(Consumer<ScopeStyle> nestedBuilder) {
    return atRule("@media (prefers-reduced-motion: no-preference)", nestedBuilder);
  }

  /// Wraps nested rules inside `@supports not (<condition>)`.

  private ScopeStyle atRule(String header, Consumer<ScopeStyle> nestedBuilder) {
    var nested = atRuleBlocks.computeIfAbsent(header, _ -> new ScopeStyle());
    nestedBuilder.accept(nested);
    return this;
  }

  public boolean isEmpty() {
    return selectorRules.values().stream().allMatch(StyleRefinement::isEmpty)
        && startingStyleRules.values().stream().allMatch(StyleRefinement::isEmpty)
        && atRuleBlocks.values().stream().allMatch(ScopeStyle::isEmpty);
  }

  public String toCssBody() {
    var sb = new StringBuilder();
    for (var entry : selectorRules.entrySet()) {
      String selector = entry.getKey();
      StyleRefinement base = entry.getValue();
      StyleRefinement starting = startingStyleRules.get(selector);
      boolean hasBase = !base.isEmpty();
      boolean hasStarting = starting != null && !starting.isEmpty();
      if (hasBase || hasStarting) {
        sb.append(selector).append('{');
        if (hasBase) {
          sb.append(base.toCssBlockBody());
        }
        if (hasStarting && starting != null) {
          if (hasBase) {
            sb.append(';');
          }
          sb.append("@starting-style{").append(starting.toCssBlockBody()).append('}');
        }
        sb.append('}');
      }
    }
    for (var entry : atRuleBlocks.entrySet()) {
      String body = entry.getValue().toCssBody();
      if (!body.isEmpty()) {
        sb.append(entry.getKey()).append('{').append(body).append('}');
      }
    }
    return sb.toString();
  }
}
