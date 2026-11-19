package dev.nthings.tessera.wasm;

import dev.nthings.tessera.view.LiveNode;

/// Captures the live author's placement and prepares callbacks to run in the root's context.
record LivePlacement(LiveNode<LiveBehavior> node) implements LiveTree {

  static LiveNode<LiveBehavior> capture(LiveContent content) {
    return switch (content.intoLiveTree()) {
      case LivePlacement placement -> placement.node();
      case LiveElement element -> element.capture();
    };
  }
}
