package dev.nthings.tessera.wasm;

import java.util.ArrayList;
import java.util.List;

/// A [Lifetime] disposed by its owning root or retained effect.
final class LifetimeScope implements Lifetime {
  private final List<Runnable> cleanups = new ArrayList<>();
  private boolean disposed;

  @Override
  public void onDispose(Runnable cleanup) {
    if (disposed) {
      cleanup.run();
      return;
    }
    cleanups.add(cleanup);
  }

  @Override
  public boolean disposed() {
    return disposed;
  }

  /// Runs every cleanup once, newest first, even when an earlier one fails. Idempotent.
  void dispose(List<RuntimeException> failures) {
    if (disposed) {
      return;
    }
    disposed = true;
    for (var index = cleanups.size() - 1; index >= 0; index--) {
      Failures.attempt(failures, cleanups.get(index));
    }
    cleanups.clear();
  }
}
