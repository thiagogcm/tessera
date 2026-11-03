package dev.nthings.tessera.core;

import dev.nthings.tessera.css.StyleSheetCollector;
import dev.nthings.tessera.css.Tokens;
import java.util.function.Supplier;

/**
 * Implicit lexical render context propagated across component boundaries via JDK 25 {@link
 * ScopedValue}.
 */
public final class RenderContext {

  private static final ScopedValue<RenderContext> CURRENT = ScopedValue.newInstance();

  private final GlobalElementId viewScope;
  private final StyleSheetCollector styles;
  private final Tokens tokens;

  public RenderContext(GlobalElementId viewScope, StyleSheetCollector styles, Tokens tokens) {
    this.viewScope = viewScope;
    this.styles = styles;
    this.tokens = tokens;
  }

  public static RenderContext current() {
    return CURRENT.orElseThrow(
        () ->
            new IllegalStateException(
                "RenderContext accessed outside an active PageCompiler pass"));
  }

  public GlobalElementId viewScope() {
    return viewScope;
  }

  public StyleSheetCollector styles() {
    return styles;
  }

  public Tokens tokens() {
    return tokens;
  }

  public <R> R withViewScope(ElementId viewEntityId, Supplier<R> work) {
    var childContext = new RenderContext(viewScope.extend(viewEntityId), styles, tokens);
    return ScopedValue.where(CURRENT, childContext).call(work::get);
  }

  public static <R> R runRoot(StyleSheetCollector styles, Tokens tokens, Supplier<R> work) {
    var rootContext = new RenderContext(GlobalElementId.ROOT, styles, tokens);
    return ScopedValue.where(CURRENT, rootContext).call(work::get);
  }
}
