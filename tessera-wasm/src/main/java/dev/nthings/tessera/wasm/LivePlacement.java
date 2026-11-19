package dev.nthings.tessera.wasm;

import dev.nthings.tessera.view.LiveNode;
import java.util.function.Function;

/// Captures the live author's placement and prepares callbacks to run in the root's context.
record LivePlacement(LiveNode<LiveBehavior> node) implements LiveTree {

  static LiveNode<LiveBehavior> capture(LiveContent content) {
    return switch (content.intoLiveTree()) {
      case LivePlacement placement -> placement.node();
      case LiveElement element -> element.capture();
    };
  }

  /// Places a component of class `type` whose body `expand` builds in the root's live context.
  static LivePlacement component(Class<?> type, Function<LiveContext, LiveContent> expand) {
    return new LivePlacement(
        new LiveNode.ComponentPlacement<>(
            type,
            LiveNode.captureSource(),
            context -> capture(expand.apply((LiveContext) context))));
  }
}
