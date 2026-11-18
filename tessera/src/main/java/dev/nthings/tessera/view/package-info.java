/// Shared rendering internals. [ViewExpansion] expands portable content and scopes, collects CSS,
/// and links references. [DocumentExpansion] lowers static placements and [ViewRenderer] lowers
/// live placements. [ViewValidation] checks linked element, reference and accessibility semantics.
/// The module exports this package only to the browser backend.
@NullMarked
@SourceBoundary
package dev.nthings.tessera.view;

import dev.nthings.tessera.internal.authoring.SourceBoundary;
import org.jspecify.annotations.NullMarked;
