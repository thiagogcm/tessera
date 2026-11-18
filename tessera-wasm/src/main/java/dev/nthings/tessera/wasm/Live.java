package dev.nthings.tessera.wasm;

import dev.nthings.tessera.view.LiveNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/// Static factories for live content that is not an element: effects and fragments. Wrap elements
/// in [LiveElement] to bind handlers or controlled properties.
public final class Live {

  private Live() {}

  /// Places `effect` at this position, keyed by `key` like [LiveContent#key(String)]. The root
  /// starts it after the first commit that contains it and keeps it running while later renders
  /// place an effect with the same `key` here; a different key disposes it and starts the new one,
  /// so the key names the effect's dependencies. Removing it, or closing the root, disposes it. The
  /// effect emits no markup. The key shares its parent's sibling namespace with keyed children, so
  /// it must differ from every other effect and keyed sibling of that parent (`duplicate-key`).
  /// Throws IllegalArgumentException if `key` is blank.
  public static LiveContent effect(String key, Effect effect) {
    var node =
        new LiveNode.Keyed<LiveBehavior>(
            key, new LiveNode.EffectPlacement<>(new LiveBehavior.Reaction(effect)));
    return new LivePlacement(node);
  }

  /// Groups live `children` as siblings without wrapper markup.
  public static LiveContent fragment(LiveContent... children) {
    return fragment(List.of(children));
  }

  /// Groups live `children` as siblings without wrapper markup, in iteration order.
  public static LiveContent fragment(Collection<? extends LiveContent> children) {
    var nodes = new ArrayList<LiveNode<LiveBehavior>>(children.size());
    for (var child : children) {
      nodes.add(LivePlacement.capture(child));
    }
    var node = new LiveNode.Fragment<>(nodes);
    return new LivePlacement(node);
  }
}
