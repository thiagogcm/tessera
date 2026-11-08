package dev.nthings.tessera.css;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/// Type-safe builder for `@scope ([data-view="..."]) to (:scope [data-view] > *)` rules in
/// `@layer components`, so [dev.nthings.tessera.core.RenderView] implementations need no raw CSS.
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

  /// Defines an open/active selector with its nested `@starting-style` entry transition.
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

  /// Emits a `@container scroll-state(<stateQuery>)` block, e.g. `stuck: top`.
  public ScopeStyle containerScrollState(String stateQuery, Consumer<ScopeStyle> nestedBuilder) {
    return container("scroll-state(" + stateQuery + ")", nestedBuilder);
  }

  /// Emits a `@container <name> scroll-state(<stateQuery>)` block.
  public ScopeStyle containerScrollState(
      CssIdent containerName, String stateQuery, Consumer<ScopeStyle> nestedBuilder) {
    return container(containerName.toCss() + " scroll-state(" + stateQuery + ")", nestedBuilder);
  }

  /// Emits a `@container <name> (inline-size >= <min>)` block.
  public ScopeStyle containerMinInlineSize(
      CssIdent containerName, CssLength minInlineSize, Consumer<ScopeStyle> nestedBuilder) {
    return container(
        containerName.toCss() + " (inline-size >= " + minInlineSize.toCss() + ")", nestedBuilder);
  }

  public ScopeStyle container(String condition, Consumer<ScopeStyle> nestedBuilder) {
    return atRule("@container " + condition, nestedBuilder);
  }

  /// Wraps nested rules inside `@media (prefers-reduced-motion: no-preference)`.
  public ScopeStyle motionSafe(Consumer<ScopeStyle> nestedBuilder) {
    return atRule("@media (prefers-reduced-motion: no-preference)", nestedBuilder);
  }

  private ScopeStyle atRule(String header, Consumer<ScopeStyle> nestedBuilder) {
    var nested = atRuleBlocks.computeIfAbsent(header, _ -> new ScopeStyle());
    nestedBuilder.accept(nested);
    return this;
  }

  public String toCssBody() {
    var sb = new StringBuilder();
    for (var entry : selectorRules.entrySet()) {
      var selector = entry.getKey();
      var base = entry.getValue();
      var starting = startingStyleRules.get(selector);
      var hasBase = !base.isEmpty();
      if (starting != null && starting.isEmpty()) {
        starting = null;
      }
      if (hasBase || starting != null) {
        sb.append(selector).append('{');
        if (hasBase) {
          sb.append(base.toCssBlockBody());
        }
        if (starting != null) {
          if (hasBase) {
            sb.append(';');
          }
          sb.append("@starting-style{").append(starting.toCssBlockBody()).append('}');
        }
        sb.append('}');
      }
    }
    for (var entry : atRuleBlocks.entrySet()) {
      var body = entry.getValue().toCssBody();
      if (!body.isEmpty()) {
        sb.append(entry.getKey()).append('{').append(body).append('}');
      }
    }
    return sb.toString();
  }
}
