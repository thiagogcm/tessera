package dev.nthings.tessera.wasm;

import dev.nthings.tessera.view.LiveNode;

/// Captures the live author's placement and prepares callbacks to run in the root's context.
record LivePlacement(LiveNode<LiveBehavior> node) implements LiveContent {

  static LiveNode<LiveBehavior> capture(LiveContent content) {
    return switch (content) {
      case LivePlacement placement -> placement.node();
      case LiveElement element -> element.capture();
      case LiveComponent component ->
          new LiveNode.ComponentPlacement<>(
              component.getClass(),
              LiveNode.captureSource(),
              context -> capture(component.expand((LiveContext) context)));
      case LiveScope scope ->
          new LiveNode.ScopePlacement<>(
              scope,
              LiveNode.captureSource(),
              context -> scope.expand((LiveContext) context).capture());
    };
  }
}
