package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.KotlinReceiver;

/// Writes selector rules into a [ScopeStyle]: the callback taken by [ScopeStyle#on(Condition.Query,
/// ScopeBlock)], which groups rules under a media feature or a container query. With Kotlin's
/// `sam-with-receiver` plugin set up for [KotlinReceiver], the scope is the lambda's receiver.
@FunctionalInterface
@KotlinReceiver
public interface ScopeBlock {

  /// Writes this block's rules into `scope`.
  void configure(ScopeStyle scope);
}
