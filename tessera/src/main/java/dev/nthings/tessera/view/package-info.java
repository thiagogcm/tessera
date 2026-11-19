/// Live-rendering facade exported only to the browser backend (`dev.nthings.tessera.wasm`).
/// [ViewRenderer] expands live placements into linked [ViewSnapshot] trees with [EmbeddedRule]s;
/// [WidgetAccess] and [ControlLowering] expose portable widget structure and control semantics.
@NullMarked
@SourceBoundary
package dev.nthings.tessera.view;

import dev.nthings.tessera.internal.authoring.SourceBoundary;
import org.jspecify.annotations.NullMarked;
