/// Backend-neutral expansion shared by the static compiler and live backends.
///
/// [ViewExpansion] turns authoring elements into [ViewNode]s: it runs component and scope expand
/// callbacks with a `DocumentContext`, binds element refs to DOM ids within id scopes and keyed
/// children, registers styles in a `StyleSheet`, and validates the contracts every backend shares.
/// A [ViewExpansion.Backend] decides what the shared expansion cannot: placement of deferred and
/// late elements, document contributions, raw HTML, effects, live styles and backend-specific
/// element checks. Linking then resolves references into a [ViewSnapshot], the tree a backend
/// emits. [ViewValidation] checks linked element, reference and accessibility semantics.
/// [Diagnostic] and [Location] carry errors in every backend, with optional page and route context.
///
/// The compiler (`compiler.Compiler`) is the static document backend. [ViewRenderer] is the backend
/// for mounted views: it renders one turn of a live root into a [ViewSnapshot] with keyed groups,
/// effects, event handlers and controlled form properties, and reports document-only features as
/// diagnostics. Types documented as "Backend-facing" are public only because backends in other
/// packages read them.
@NullMarked
@SourceBoundary
package dev.nthings.tessera.view;

import dev.nthings.tessera.core.SourceBoundary;
import org.jspecify.annotations.NullMarked;
