package dev.nthings.tessera.core;

/// Any value that can be added as a child: an [Element], or a component such as [RenderOnce] or
/// [RenderView].
@FunctionalInterface
public interface IntoElement {

  /// Lowers this value to an [Element]. Builders call it when the value is added to a parent.
  Element intoElement();
}
