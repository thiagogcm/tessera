package dev.nthings.tessera.wasm;

import dev.nthings.tessera.view.LiveNode;

/// Component that produces live content: event handlers, controlled form properties or effects. It
/// receives a [LiveContext] and can be placed only in a browser root, including as the root's view
/// itself. Portable components placed inside it still receive only a portable context.
@FunctionalInterface
public interface LiveComponent extends LiveContent {

  /// Builds the component's content for one render. Don't keep `context` after the call returns.
  LiveContent expand(LiveContext context);

  @Override
  default LiveTree intoLiveTree() {
    return new LivePlacement(
        new LiveNode.ComponentPlacement<>(
            getClass(),
            LiveNode.captureSource(),
            context -> LivePlacement.capture(expand((LiveContext) context))));
  }
}
