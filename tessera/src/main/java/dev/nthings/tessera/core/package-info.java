/// Portable element builders, component contracts, identities and diagnostics.
///
/// Portable content works in static pages and browser roots. Build trees with [Elements], write
/// reusable content as a [Component] or [Scope], and bind and link element identities with
/// [ElementRef]. Both component contracts receive a [ViewContext], which keeps [ViewKey] state for
/// one expansion and reports diagnostics. [IntoElement#key(String)] gives content a stable identity
/// among its siblings.
///
/// Document assembly lives in `dev.nthings.tessera.document`. Live authoring, handlers and effects
/// live in the optional browser module.
@NullMarked
package dev.nthings.tessera.core;

import org.jspecify.annotations.NullMarked;
