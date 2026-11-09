package dev.nthings.tessera.css;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/// The styles of one view scope: the `@scope` block around a
/// [dev.nthings.tessera.core.View]'s root, in `@layer components`. Authors receive it in
/// [dev.nthings.tessera.core.View#configureScope(ScopeStyle)] and fill it; they do not
/// construct it. Selectors stop at nested views, so the outer view's rules don't reach inside them.
///
/// Each rule's [StyleRefinement] can also hold conditional styles
/// ([Styled#on(Condition, StyleBlock)]) and a starting style
/// ([StyleRefinement#startingStyle(StyleBlock)]), which nest inside that rule.
/// [#on(Condition.Query, ScopeBlock)] groups whole rules under a media feature or a container
/// query.
public final class ScopeStyle {

  private final Map<String, StyleRefinement> selectorRules = new LinkedHashMap<>();
  private final Map<Condition.Query, ScopeStyle> queryBlocks = new LinkedHashMap<>();

  /// Created by [StyleSheet#registerViewScope(String, ScopeBlock)].
  ScopeStyle() {}

  /// Styles the view's root element, `:scope`.
  public ScopeStyle root(StyleBlock styles) {
    return select(":scope", styles);
  }

  /// Styles the elements matching `selector` inside the view scope. The selector is a raw CSS
  /// selector list: it is not validated and is emitted as written, so use `:scope` to refer to the
  /// view root. Calling again with the same selector extends the same rule.
  public ScopeStyle select(String selector, StyleBlock styles) {
    styles.configure(selectorRules.computeIfAbsent(selector, _ -> new StyleRefinement()));
    return this;
  }

  /// Wraps the rules `rules` writes in an `@media` or `@container` block for `query`, such as
  /// `on(Media.MOTION_SAFE, ...)` or `on(container.atLeast(CssLength.rem(48)), ...)`. Calling
  /// again with an equal query extends the same block. Blocks are emitted after the scope's plain
  /// rules, in first-use order.
  public ScopeStyle on(Condition.Query query, ScopeBlock rules) {
    rules.configure(queryBlocks.computeIfAbsent(query, _ -> new ScopeStyle()));
    return this;
  }

  /// Every keyframes rule the scope's styles use.
  void collectKeyframes(Set<Keyframes> target) {
    selectorRules.values().forEach(style -> style.collectKeyframes(target));
    queryBlocks.values().forEach(block -> block.collectKeyframes(target));
  }

  /// The rule text for the view scope's `@scope` block, without the block.
  String toCssBody() {
    var sb = new StringBuilder();
    selectorRules.forEach(
        (selector, style) -> {
          if (!style.isEmpty()) {
            sb.append(selector).append('{').append(style.toCssBody()).append('}');
          }
        });
    queryBlocks.forEach(
        (query, block) -> {
          var body = block.toCssBody();
          if (!body.isEmpty()) {
            sb.append(StyleRefinement.prelude(query)).append('{').append(body).append('}');
          }
        });
    return sb.toString();
  }
}
