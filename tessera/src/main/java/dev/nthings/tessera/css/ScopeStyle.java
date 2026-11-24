package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.LinkedHashMap;
import java.util.Map;

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

  private final Map<String, Declarations> selectorRules = new LinkedHashMap<>();
  private final Map<Condition.Query, ScopeStyle> queryBlocks = new LinkedHashMap<>();

  /// The conditions an enclosing block already requires.
  private final Enclosing enclosing;

  ScopeStyle() {
    this(Enclosing.NONE);
  }

  private ScopeStyle(Enclosing enclosing) {
    this.enclosing = enclosing;
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
    styles.configure(selectorRules.computeIfAbsent(selector, _ -> new Declarations(enclosing)));
    return this;
  }

  /// Wraps the rules `rules` writes in an `@media` or `@container` block for `query`, such as
  /// `on(Media.MOTION_SAFE, ...)` or `on(container.atLeast(CssLength.rem(48)), ...)`. Calling again
  /// with an equal query extends the same block. Blocks are emitted after the scope's plain rules,
  /// in first-use order. Inside a block for an equal query, the rules join the enclosing block.
  public ScopeStyle on(Condition.Query query, ScopeBlock rules) {
    rules.configure(
        enclosing.enter(
            query,
            this,
            inside -> queryBlocks.computeIfAbsent(query, _ -> new ScopeStyle(inside))));
    return this;
  }

  /// Writes the rules of the id scope's `@scope` block, without the block, into `sink`.
  void write(CssSink sink) {
    selectorRules.forEach((selector, style) -> {
      sink.open(selector);
      style.writeBody(sink);
      sink.close();
    });
    queryBlocks.forEach((query, block) -> {
      sink.open(Declarations.prelude(query));
      block.write(sink);
      sink.close();
    });
  }
}
