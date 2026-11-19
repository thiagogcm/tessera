package dev.nthings.tessera.wasm;

/// Package-private base for built-in adaptive widgets so `expand` stays off their public API.
abstract non-sealed class BuiltinLiveComponent implements LiveContent {
  abstract LiveContent expand(LiveContext context);
}
