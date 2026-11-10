package dev.nthings.tessera.wasm;

/// Handle to scheduled work or an in-flight request owned by a [Host].
@FunctionalInterface
public interface Cancellable {

  /// Stops the work; its callback never runs afterwards. Idempotent.
  void cancel();
}
