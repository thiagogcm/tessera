/// Authoring model: element builders, component contracts, identities and the render context.
/// Build trees with the static factories in [Elements], which return mutable [TagElement] builders,
/// [FragmentElement] or plain [Element]. Write components as [Component] (no view scope of its own)
/// or [View] (own id namespace and scoped CSS); both return any [IntoElement] from `render`. Give
/// elements ids and link them with [ElementRef], run invoker commands with [Command], and
/// contribute head content, client behaviors, headings and diagnostics through the
/// [RenderContext] passed to render callbacks. Compile with `compiler.Compiler`; here, "document"
/// means the page being compiled. Types documented as "Compiler-facing" are public only because
/// the compiler reads them.
@NullMarked
package dev.nthings.tessera.core;

import org.jspecify.annotations.NullMarked;
