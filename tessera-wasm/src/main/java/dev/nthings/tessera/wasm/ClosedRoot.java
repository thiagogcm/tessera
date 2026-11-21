package dev.nthings.tessera.wasm;

/// Signals that a browser root started closing while a commit was applying a candidate. The closing
/// root released what the commit acquired along with what it already owned, so the caller publishes
/// nothing, starts nothing and reports no failure.
final class ClosedRoot extends RuntimeException {
  private static final long serialVersionUID = 1L;

  ClosedRoot() {
    super(null, null, false, false);
  }
}
