package dev.nthings.tessera.core;

/// What every expand callback can do beyond returning content: keep state for the current expansion
/// and report authoring errors. Portable [Component] and [Scope] callbacks receive it in both
/// backends. `DocumentContext` adds document assembly for static pages, and the browser module's
/// live context adds its root and services; a portable callback never receives either capability.
///
/// Don't keep a context after the callback returns, and don't share mutable application state
/// through it: callbacks may run concurrently across pages.
public interface ViewContext {

  /// State for `key` within this expansion, created on first use. An expansion is one compiled page
  /// or one attempted browser render; a rejected render discards its state. Use it for facts that
  /// must hold once per expansion, such as a counter that numbers instances in encounter order.
  <T> T state(ViewKey<T> key);

  /// Reports an authoring error found by a component, such as two options that cannot be combined.
  /// `rule` is a short kebab-case code, `message` says what is wrong and `fix` how to correct it.
  /// The diagnostic is located at the current component and rejects the expansion like the
  /// library's own rules.
  void report(String rule, String message, String fix);
}
