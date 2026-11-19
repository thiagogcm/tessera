/// Live rendering exported only to the browser backend (`dev.nthings.tessera.wasm`). [ViewRenderer]
/// expands [LiveNode] placements into a [ViewSnapshot]: the linked `internal.view.LinkedNode` tree
/// and its embedded `internal.css.StyleSheet.Rule`s. The backend reads widget structure, control
/// semantics and embedded-style constants from the internal packages exported to it.
@NullMarked
@SourceBoundary
package dev.nthings.tessera.view;

import dev.nthings.tessera.internal.authoring.SourceBoundary;
import org.jspecify.annotations.NullMarked;
