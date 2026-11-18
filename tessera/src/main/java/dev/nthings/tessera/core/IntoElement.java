package dev.nthings.tessera.core;

import dev.nthings.tessera.internal.authoring.PortableNode;

/// Portable content, placeable as a child of any portable builder: an [Element], a [Component], a
/// [Scope], or a built-in component. Expand callbacks return it too, so a component can return
/// another component directly. Document and live builders accept it as well; it never accepts their
/// content in return.
@FunctionalInterface
public interface IntoElement {

  /// Lowers this value to an [Element]. Builders call it when the value is added to a parent.
  Element intoElement();

  /// Gives this content a stable identity among its siblings, so a live backend keeps its DOM,
  /// focus and form state when siblings are inserted, removed or reordered, and replaces it when
  /// the key changes. Ids bound inside it, generated ones and [ElementRef#scoped(String)] alike,
  /// are scoped by `key` and the nearest enclosing element bound to a named ref, instead of by
  /// position, so an item's ids and the links to them stay the same wherever it moves. The content
  /// is emitted in place, without a wrapper element.
  ///
  /// Keys are compared exactly and only among the children of one parent: give each sibling a
  /// distinct key, or the page fails with `duplicate-key`. Calling `key` again replaces the key.
  /// Throws IllegalArgumentException if `key` is blank.
  default Element key(String key) {
    return new PortableNode.Keyed(key, intoElement());
  }
}
