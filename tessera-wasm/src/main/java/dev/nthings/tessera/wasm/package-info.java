/// Experimental browser backend: mounts Tessera components in a page as Java compiled to
/// WebAssembly with GraalVM Web Image.
///
/// [BrowserRoot#mount] renders a view callback into a host element and re-renders it after
/// [BrowserRoot#invalidate()] or a [BrowserRoot#update(Runnable)] wrapper runs, reconciling the
/// native DOM by sibling key and kind, so focus, selection and form state survive updates. Event
/// handlers bound with `TagElement.onEvent` and `Button.onClick` run synchronously in the browser's
/// dispatch; controlled form properties, live styles and effects work as documented on
/// `TagElement` and `Elements.effect`. [Host] gives element operations (focus, dialogs, popovers,
/// media), timers and requests bound to a root or effect lifetime. [BrowserEntry] mounts the hosts
/// a page declares for one compiled entry point.
///
/// Rendering goes through `view.ViewRenderer`, so a mounted view expands, links and validates
/// exactly like a compiled page, except that document-only features (head contributions,
/// document-end sections, client behaviors, deferred and late elements, raw HTML) are reported as
/// diagnostics and reject the render.
@NullMarked
package dev.nthings.tessera.wasm;

import org.jspecify.annotations.NullMarked;
