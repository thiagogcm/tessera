package dev.nthings.tessera.css;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/// The styles of one view scope: the `@scope` block around a
/// [dev.nthings.tessera.core.View]'s root, in `@layer components`. Authors receive it in
/// [dev.nthings.tessera.core.View#configureScope(ScopeStyle)] and fill it; they do not
/// construct it. Selectors stop at nested views, so the outer view's rules don't reach inside them.
public final class ScopeStyle {

  private final Map<String, StyleRefinement> selectorRules = new LinkedHashMap<>();
  private final Map<String, StyleRefinement> startingStyleRules = new LinkedHashMap<>();
  private final Map<String, ScopeStyle> atRuleBlocks = new LinkedHashMap<>();

  /// Styles the view's root element, `:scope`.
  public ScopeStyle root(Consumer<StyleRefinement> styles) {
    return select(":scope", styles);
  }

  /// Styles the elements matching `selector` inside the view scope. The selector is not validated;
  /// it is emitted as written. Calling again with the same selector extends the same rule.
  public ScopeStyle select(String selector, Consumer<StyleRefinement> styles) {
    styles.accept(selectorRules.computeIfAbsent(selector, _ -> new StyleRefinement()));
    return this;
  }

  /// Styles `selector` with `openStyles` and nests `startingStyles` in its `@starting-style`
  /// block, the values an entry transition starts from.
  public ScopeStyle selectWithStartingStyle(
      String selector,
      Consumer<StyleRefinement> openStyles,
      Consumer<StyleRefinement> startingStyles) {
    select(selector, openStyles);
    startingStyles.accept(startingStyleRules.computeIfAbsent(selector, _ -> new StyleRefinement()));
    return this;
  }

  /// Styles written by `nestedBuilder` apply inside `@container (inline-size >= minInlineSize)`,
  /// checked against the nearest ancestor container (see [Styled#containerInlineSize()]).
  /// Paired with [#containerMaxInlineSize], the two ranges do not overlap.
  public ScopeStyle containerMinInlineSize(
      CssLength minInlineSize, Consumer<ScopeStyle> nestedBuilder) {
    return container("(inline-size >= " + minInlineSize.toCss() + ")", nestedBuilder);
  }

  /// Styles written by `nestedBuilder` apply inside `@container (inline-size < maxInlineSize)`,
  /// checked against the nearest ancestor container. Paired with [#containerMinInlineSize], the
  /// two ranges do not overlap.
  public ScopeStyle containerMaxInlineSize(
      CssLength maxInlineSize, Consumer<ScopeStyle> nestedBuilder) {
    return container("(inline-size < " + maxInlineSize.toCss() + ")", nestedBuilder);
  }

  /// Emits a `@container scroll-state(<stateQuery>)` block, e.g. `stuck: top`. `stateQuery` is
  /// copied into the CSS as written.
  public ScopeStyle containerScrollState(String stateQuery, Consumer<ScopeStyle> nestedBuilder) {
    return container("scroll-state(" + stateQuery + ")", nestedBuilder);
  }

  /// Emits a `@container <name> scroll-state(<stateQuery>)` block. `stateQuery` is copied into the
  /// CSS as written.
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

  /// Wraps the rules `nestedBuilder` writes in `@container <condition>`. The condition is copied
  /// into the CSS as written.
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

  /// The rule text for the view scope's `@scope` block, without the block.
  String toCssBody() {
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
