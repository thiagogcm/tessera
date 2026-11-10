package dev.nthings.tessera.core;

/// Work bound to a position in a live tree, placed with [Elements#effect(String, Effect)]: a
/// timer, a subscription, a request. Only a live backend runs effects; static compilation reports
/// them as `native-effect`.
@FunctionalInterface
public interface Effect {

  /// Starts the work after the tree containing the effect is committed. Register cleanup on
  /// `lifetime`; it runs when the effect is removed, its key changes or its root closes.
  void start(Lifetime lifetime);
}
