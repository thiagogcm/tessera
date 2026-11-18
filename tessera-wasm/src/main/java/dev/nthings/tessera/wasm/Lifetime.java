package dev.nthings.tessera.wasm;

/// A scope whose cleanups run once, in reverse registration order, when its owner is disposed. A
/// browser root and each started [Effect] own one: register listeners, subscriptions, timers and
/// requests here, so disposing the owner releases them.
public interface Lifetime {

  /// Registers `cleanup`. After disposal it runs immediately instead.
  void onDispose(Runnable cleanup);

  /// Whether the owner has been disposed.
  boolean disposed();

  /// Wraps an asynchronous completion so that it runs only while this lifetime is active. Wrap a
  /// callback when the work starts, so a late result cannot touch a disposed owner.
  default Runnable guard(Runnable completion) {
    return () -> {
      if (!disposed()) {
        completion.run();
      }
    };
  }
}
