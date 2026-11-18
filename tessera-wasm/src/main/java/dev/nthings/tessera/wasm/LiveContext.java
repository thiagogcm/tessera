package dev.nthings.tessera.wasm;

import dev.nthings.tessera.core.ViewContext;

/// The [ViewContext] of live callbacks: a [BrowserRoot]'s view and its [LiveComponent] and
/// [LiveScope] expansions. Besides expansion state and diagnostics, it gives the owning root, whose
/// invalidation handle and [Host] services the callback may capture in event handlers and effects.
/// Expansion state lasts one attempted render: a rejected render discards it. Keep application
/// state outside render callbacks, and start timers, subscriptions and requests in effects or event
/// handlers, never during expansion. Portable components placed inside live content still receive
/// only a [ViewContext].
public interface LiveContext extends ViewContext {

  /// The root this render belongs to. Capture it in handlers and effects to invalidate it, wrap
  /// updates with [BrowserRoot#update(Runnable)] or reach its [BrowserRoot#host()] services.
  BrowserRoot root();
}
