package dev.nthings.tessera.wasm;

/// Work bound to a position in a live tree, placed with the browser module's `Live.effect`: a
/// timer, a subscription, a request. Only a browser root runs effects; document content cannot
/// contain them.
@FunctionalInterface
public interface Effect {

  /// Starts the work after the tree containing the effect is committed. Register cleanup on
  /// `lifetime`; it runs when the effect is removed, its key changes or its root closes.
  void start(Lifetime lifetime);
}
