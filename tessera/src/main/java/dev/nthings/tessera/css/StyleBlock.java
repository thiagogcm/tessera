package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.KotlinReceiver;

/// Writes declarations into a [Declarations]: the callback taken by
/// [Styled#on(Condition, StyleBlock)], [ScopeStyle#select(String, StyleBlock)],
/// [Declarations#startingStyle(StyleBlock)] and the [Keyframes] steps, and the type of a
/// reusable style preset applied with [Styled#styles(StyleBlock)]:
///
/// ```java
/// StyleBlock card = s -> s.p(Tokens.SPACE_4).rounded(Tokens.RADIUS_XL);
/// div().styles(card).on(State.HOVER, s -> s.bg(Tokens.SURFACE_SUBTLE));
/// ```
///
/// With Kotlin's `sam-with-receiver` plugin set up for [KotlinReceiver], the refinement is the
/// lambda's receiver: `div().on(State.HOVER) { bg(Tokens.SURFACE_SUBTLE) }`.
@FunctionalInterface
@KotlinReceiver
public interface StyleBlock {

  /// Writes this block's declarations into `style`.
  void configure(Declarations style);
}
