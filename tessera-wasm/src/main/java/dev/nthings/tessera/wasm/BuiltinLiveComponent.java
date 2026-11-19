package dev.nthings.tessera.wasm;

import dev.nthings.tessera.view.LiveNode;

/// Package-private base for built-in adaptive widgets so `expand` stays off their public API.
abstract class BuiltinLiveComponent implements LiveContent {

  abstract LiveContent expand(LiveContext context);

  @Override
  public final LiveTree intoLiveTree() {
    return new LivePlacement(
        new LiveNode.ComponentPlacement<>(
            getClass(),
            LiveNode.captureSource(),
            context -> LivePlacement.capture(expand((LiveContext) context))));
  }
}
