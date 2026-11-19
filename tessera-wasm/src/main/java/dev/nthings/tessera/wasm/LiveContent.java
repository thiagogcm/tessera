package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.view.LiveNode;

/// Content that only a browser root places: elements with Java event handlers or controlled form
/// properties ([LiveElement]), effects ([Live#effect]), live components and scopes, custom live
/// builders, and portable content converted with [#of(IntoElement)].
@FunctionalInterface
public interface LiveContent {

  /// Lowers this value to a [LiveTree]. Live builders call it when the value is added to a parent.
  LiveTree intoLiveTree();

  /// Portable `content` placed as live content. It is lowered when this method is called.
  static LiveTree of(IntoElement content) {
    return new LivePlacement(new LiveNode.Portable<>(content.intoElement()));
  }

  /// Gives this content a stable identity among its siblings, so a browser root keeps its DOM,
  /// focus and form state when siblings are inserted, removed or reordered, and replaces it when
  /// the key changes. Ids bound inside it are scoped by the key, as [IntoElement#key(String)]
  /// describes. Calling `key` again replaces the key. Throws IllegalArgumentException if `key` is
  /// blank.
  default LiveTree key(String key) {
    return new LivePlacement(LivePlacement.capture(this).key(key));
  }
}
