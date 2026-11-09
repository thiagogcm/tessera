package dev.nthings.tessera.core;

/// Anything placeable as a child: an [Element], a [Component], a [View], or a built-in component.
/// Render callbacks return it too, so a component can return another component directly.
@FunctionalInterface
public interface IntoElement {

  /// Lowers this value to an [Element]. Builders call it when the value is added to a parent.
  Element intoElement();
}
