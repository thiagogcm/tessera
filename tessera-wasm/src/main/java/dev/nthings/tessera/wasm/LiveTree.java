package dev.nthings.tessera.wasm;

/// Lowered live content produced by [LiveElement], [Live] factories, [LiveComponent], [LiveScope],
/// and built-in live widgets. The hierarchy is closed; custom live builders implement [LiveContent]
/// instead.
public sealed interface LiveTree extends LiveContent permits LiveElement, LivePlacement {

  @Override
  default LiveTree intoLiveTree() {
    return this;
  }
}
