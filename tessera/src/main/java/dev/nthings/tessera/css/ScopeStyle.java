package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.FluentBuilder;
import dev.nthings.tessera.internal.foundation.Fnv1a64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/// The styles of one id scope: the `@scope` block around a [dev.nthings.tessera.core.Scope]'s root,
/// in `@layer components`. Authors receive it in
/// [dev.nthings.tessera.core.Scope#configureScope(ScopeStyle)] and fill it; they do not construct
/// it. Selectors stop at nested scopes, so an outer scope's rules don't reach inside a nested one.
///
/// Each rule's [Declarations] can also hold conditional styles ([Styled#on(Condition, StyleBlock)])
/// and a starting style ([Declarations#startingStyle(StyleBlock)]), which nest inside that rule.
/// [#on(Condition.Query, ScopeBlock)] groups whole rules under a media feature or a container
/// query.
public final class ScopeStyle implements FluentBuilder<ScopeStyle> {

  // Markers that make the structural hash prefix-free.
  private static final int BLOCK_END = 0;
  private static final int SELECTOR_RULE = 1;
  private static final int QUERY_BLOCK = 2;

  private final Map<String, Declarations> selectorRules = new LinkedHashMap<>();
  private final Map<Condition.Query, ScopeStyle> queryBlocks = new LinkedHashMap<>();

  /// The [Media] features an enclosing block already requires, as a bit set of their ordinals.
  private final int enclosingMedia;

  ScopeStyle() {
    this(0);
  }

  private ScopeStyle(int enclosingMedia) {
    this.enclosingMedia = enclosingMedia;
  }

  @Override
  public ScopeStyle self() {
    return this;
  }

  /// Styles the scope's root element, `:scope`.
  public ScopeStyle root(StyleBlock styles) {
    return select(":scope", styles);
  }

  /// Styles the elements matching `selector` inside the id scope. The selector is a raw CSS
  /// selector list: it is not validated and is emitted as written, so use `:scope` to refer to the
  /// scope root. Calling again with the same selector extends the same rule.
  public ScopeStyle select(String selector, StyleBlock styles) {
    styles.configure(
        selectorRules.computeIfAbsent(selector, _ -> new Declarations(enclosingMedia)));
    return this;
  }

  /// Wraps the rules `rules` writes in an `@media` or `@container` block for `query`, such as
  /// `on(Media.MOTION_SAFE, ...)` or `on(container.atLeast(CssLength.rem(48)), ...)`. Calling again
  /// with an equal query extends the same block. Blocks are emitted after the scope's plain rules,
  /// in first-use order. Inside a block for the same media feature, the rules join the enclosing
  /// block.
  public ScopeStyle on(Condition.Query query, ScopeBlock rules) {
    var media = enclosingMedia;
    if (query instanceof Media feature) {
      var bit = Declarations.mediaBit(feature);
      if ((media & bit) != 0) {
        rules.configure(this);
        return this;
      }
      media |= bit;
    }
    var blockMedia = media;
    rules.configure(queryBlocks.computeIfAbsent(query, _ -> new ScopeStyle(blockMedia)));
    return this;
  }

  /// Every keyframes rule the scope's styles use.
  void collectKeyframes(Set<Keyframes> target) {
    selectorRules.values().forEach(style -> style.collectKeyframes(target));
    queryBlocks.values().forEach(block -> block.collectKeyframes(target));
  }

  /// Whether no rule emits anything, so [#toCssBody()] is empty.
  boolean isEmpty() {
    for (var style : selectorRules.values()) {
      if (!style.isEmpty()) {
        return false;
      }
    }
    for (var block : queryBlocks.values()) {
      if (!block.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /// Folds everything [#toCssBody()] serializes into `hash`, in the same order, without serializing
  /// it, so equal scopes fold equal hashes.
  long updateHash(long hash) {
    var h = hash;
    for (var entry : selectorRules.entrySet()) {
      if (!entry.getValue().isEmpty()) {
        h = Fnv1a64.updateByte(h, SELECTOR_RULE);
        h = Declarations.updateText(h, entry.getKey());
        h = entry.getValue().updateBody(h);
      }
    }
    for (var entry : queryBlocks.entrySet()) {
      if (!entry.getValue().isEmpty()) {
        h = Fnv1a64.updateByte(h, QUERY_BLOCK);
        h = Declarations.updateText(h, Declarations.prelude(entry.getKey()));
        h = entry.getValue().updateHash(h);
      }
    }
    return Fnv1a64.updateByte(h, BLOCK_END);
  }

  /// The rule text for the id scope's `@scope` block, without the block.
  String toCssBody() {
    var sb = new StringBuilder();
    selectorRules.forEach((selector, style) -> {
      if (!style.isEmpty()) {
        sb.append(selector).append('{').append(style.toCssBody()).append('}');
      }
    });
    queryBlocks.forEach((query, block) -> {
      var body = block.toCssBody();
      if (!body.isEmpty()) {
        sb.append(Declarations.prelude(query)).append('{').append(body).append('}');
      }
    });
    return sb.toString();
  }
}
