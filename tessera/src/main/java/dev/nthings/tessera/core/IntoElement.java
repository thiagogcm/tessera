package dev.nthings.tessera.core;

/// Anything placeable as a child: an [Element], a [Component], a [View], or a built-in component.
/// Render callbacks return it too, so a component can return another component directly.
@FunctionalInterface
public interface IntoElement {

  /// Lowers this value to an [Element]. Builders call it when the value is added to a parent.
  Element intoElement();

  /// Gives this content a stable identity among its siblings, for lists whose items are inserted,
  /// removed or reordered between builds. Ids bound inside it, generated ones and
  /// [ElementRef#scoped(String)] alike, are scoped by `key` instead of by position, so an item's
  /// ids and the links to them stay the same wherever it moves. The content is emitted in place,
  /// without a wrapper element.
  ///
  /// Keys are compared exactly and only among the children of one parent: give each sibling a
  /// distinct key, or the page fails with `duplicate-key`. Calling `key` again replaces the key.
  /// Throws IllegalArgumentException if `key` is blank.
  default Element key(String key) {
    return new KeyedElement(key, intoElement());
  }
}
