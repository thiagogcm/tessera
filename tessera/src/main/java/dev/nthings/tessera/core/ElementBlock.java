package dev.nthings.tessera.core;

import dev.nthings.tessera.foundation.KotlinReceiver;

/// Configures an element that a component owns, such as the `<img>` of
/// `dev.nthings.tessera.components.Image` or the tooltip of [TagElement#interestTooltip]:
/// attributes, ARIA states, styles and refs, through the same [TagElement] methods as any other
/// element.
///
/// ```java
/// Image.of(src, "").dimensions(64, 64).element(img -> img.ariaHidden(true).rounded(radius));
/// ```
///
/// With Kotlin's `sam-with-receiver` plugin set up for [KotlinReceiver], the element is the
/// lambda's receiver: `.element { ariaHidden(true) }`.
@FunctionalInterface
@KotlinReceiver
public interface ElementBlock {

  /// Configures `element`.
  void configure(TagElement element);
}
