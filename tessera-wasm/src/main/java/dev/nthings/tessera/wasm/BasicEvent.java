package dev.nthings.tessera.wasm;

/// A native browser event without specialized pointer, keyboard, or control accessors (such as
/// `submit`, `reset`, `focus`, `close`, or custom events).
public sealed interface BasicEvent extends BrowserEvent permits DomReconciler.NativeBasicEvent {}
