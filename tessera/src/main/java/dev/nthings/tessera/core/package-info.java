/// Authoring model: element builders, component contracts, identities and the document context.
/// Build trees with the static factories in [Elements], which return mutable [TagElement] builders,
/// [FragmentElement] or plain [Element]. Write components as [Component] (no id scope of its own)
/// or [Scope] (own id namespace and scoped CSS); both return any [IntoElement] from `expand`. Give
/// elements ids and link them with [ElementRef], run invoker commands with [Command], and
/// contribute head content, page scripts, headings and diagnostics through the [DocumentContext]
/// passed to expand callbacks. Compile with `compiler.Compiler`; here, "document" means the page
/// being compiled.
///
/// For live backends such as the `tessera-wasm` browser mount, elements also take Java event
/// handlers ([TagElement#onEvent], receiving a [BrowserEvent]), controlled form properties
/// ([TagElement#value(String)]) and live styles ([TagElement#live]); [Elements#effect] places an
/// [Effect] bound to a [Lifetime], and [IntoElement#key(String)] gives content a stable identity
/// among its siblings. Static compilation rejects handlers, controlled properties and effects with
/// diagnostics. Types documented as "Compiler-facing" or "Backend-facing" are public only because
/// the compiler or a backend reads them.
@NullMarked
package dev.nthings.tessera.core;

import org.jspecify.annotations.NullMarked;
