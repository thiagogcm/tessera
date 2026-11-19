package dev.nthings.tessera.view;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.internal.css.StyleSheet;
import dev.nthings.tessera.internal.view.LinkedNode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Backend-facing: the linked view tree of one live render ([ViewRenderer]), with its generated
/// CSS. Linking has resolved element references, control defaults and tooltip companions, so every
/// node is ready for DOM reconciliation.
///
/// - `roots`: the top-level nodes.
/// - `styleRules`: this render's self-contained embedded rules, each with a short key that
///   identifies its text; an embedder may insert each one independently, deduplicate them across
///   roots and keep them, by key, for later renders.
/// - `references`: each element ref bound in this tree and its DOM id, so a backend can find the
///   committed element of a ref.
/// - `ids`: every DOM id the tree renders, generated or explicit, so a backend can detect
///   collisions with other roots.
public record ViewSnapshot<M>(
    List<LinkedNode<M>> roots,
    List<StyleSheet.Rule> styleRules,
    Map<ElementRef, String> references,
    Set<String> ids) {

  public ViewSnapshot {
    // Linking and the stylesheet hand over fresh lists and expansion its identity maps; a render
    // avoids copying them every turn.
    roots = Collections.unmodifiableList(roots);
    styleRules = Collections.unmodifiableList(styleRules);
    references = Collections.unmodifiableMap(references);
    ids = Collections.unmodifiableSet(ids);
  }
}
