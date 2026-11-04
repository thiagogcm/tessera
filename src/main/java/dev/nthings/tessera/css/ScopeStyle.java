package dev.nthings.tessera.css;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Type-safe builder for {@code @scope ([data-view="..."]) to ([data-view])} rules in {@code @layer
 * components}.
 *
 * <p>Eliminates raw CSS strings inside {@link dev.nthings.tessera.core.RenderView} implementations
 * by expressing `:scope`, descendant selectors, and {@code @container} queries through {@link
 * StyleRefinement}.
 */
public final class ScopeStyle {

  private final Map<String, StyleRefinement> selectorRules = new LinkedHashMap<>();
  private final Map<String, ScopeStyle> containerQueries = new LinkedHashMap<>();

  public ScopeStyle root(UnaryOperator<StyleRefinement> mutator) {
    return select(":scope", mutator);
  }

  public ScopeStyle select(String selector, UnaryOperator<StyleRefinement> mutator) {
    var refinement = selectorRules.computeIfAbsent(selector, _ -> new StyleRefinement());
    mutator.apply(refinement);
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

  public ScopeStyle container(String condition, Consumer<ScopeStyle> nestedBuilder) {
    var nested = containerQueries.computeIfAbsent(condition, _ -> new ScopeStyle());
    nestedBuilder.accept(nested);
    return this;
  }

  public boolean isEmpty() {
    return selectorRules.values().stream().allMatch(StyleRefinement::isEmpty)
        && containerQueries.values().stream().allMatch(ScopeStyle::isEmpty);
  }

  public String toCssBody() {
    var sb = new StringBuilder();
    for (var entry : selectorRules.entrySet()) {
      if (!entry.getValue().isEmpty()) {
        sb.append(entry.getKey()).append('{').append(entry.getValue().toCssBlockBody()).append('}');
      }
    }
    for (var entry : containerQueries.entrySet()) {
      if (!entry.getValue().isEmpty()) {
        sb.append("@container ")
            .append(entry.getKey())
            .append('{')
            .append(entry.getValue().toCssBody())
            .append('}');
      }
    }
    return sb.toString();
  }
}
