/// Authoring model: element builders, component contracts, identities and the render context.
/// Build trees with the static factories in [Elements], which return mutable [TagElement] builders
/// and [FragmentElement]. Write components as [RenderOnce] (no view scope of its own) or
/// [RenderView] (own view scope and scoped CSS), link elements with [ElementRef], and contribute
/// head content, client behaviors and headings through the [RenderContext] passed to render
/// callbacks. Compile with `render.PageCompiler` or `site.SiteCompiler`; here, "document" means
/// the page being compiled.
@NullMarked
package dev.nthings.tessera.core;

import org.jspecify.annotations.NullMarked;
