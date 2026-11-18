package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.css.ScopeStyle;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Shares a built-in component's scope identity and styles with its browser adapter.
record WidgetScope(Scope portable, Function<LiveContext, LiveElement> render) implements LiveScope {

  @Override
  public @Nullable String scopeId() {
    return portable.scopeId();
  }

  @Override
  public String scopeName() {
    return portable.scopeName();
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    portable.configureScope(scope);
  }

  @Override
  public LiveElement expand(LiveContext context) {
    return render.apply(context);
  }
}
