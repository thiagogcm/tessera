package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.core.ViewKey;

/// The [LiveContext] of one render callback: the portable operations of its expansion context and
/// the owning root.
final class RootContext implements LiveContext {

  private final ViewContext base;
  private final BrowserRoot root;

  RootContext(ViewContext base, BrowserRoot root) {
    this.base = base;
    this.root = root;
  }

  @Override
  public <T> T state(ViewKey<T> key) {
    return base.state(key);
  }

  @Override
  public void report(String rule, String message, String fix) {
    base.report(rule, message, fix);
  }

  @Override
  public BrowserRoot root() {
    return root;
  }
}
