/// Experimental browser backend: mounts Tessera components in a page as Java compiled to
/// WebAssembly with GraalVM Web Image.
///
/// [BrowserRoot#mount] renders a [LiveComponent] view into a host element and re-renders it after
/// [BrowserRoot#invalidate()] or a [BrowserRoot#update(Runnable)] wrapper runs, reconciling the
/// native DOM by sibling key and kind, so focus, selection and form state survive updates. Views
/// return [LiveContent]: portable content plus [LiveElement] hosts with Java event handlers and
/// controlled form properties, [Live#effect] placements bound to a lifetime, and [LiveComponent]
/// and [LiveScope] callbacks that receive a [LiveContext]. Live content cannot be placed in
/// portable or document content, and document content cannot be placed here. [Host] gives element
/// operations (focus, dialogs, popovers, media), timers and requests bound to a root or effect
/// lifetime. [BrowserEntry] mounts the hosts a page declares for one compiled entry point.
///
/// Rendering goes through `view.ViewRenderer`, so a mounted view expands, links and validates
/// portable content exactly like a compiled page. Bundled widget scripts that portable elements
/// declare for documents are not loaded.
@NullMarked
package dev.nthings.tessera.wasm;

import org.jspecify.annotations.NullMarked;
