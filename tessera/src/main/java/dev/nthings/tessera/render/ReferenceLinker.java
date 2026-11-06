package dev.nthings.tessera.render;

import dev.nthings.tessera.render.ResolvedNode.ResolvedSlot;
import dev.nthings.tessera.render.ResolvedNode.ResolvedTag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// Resolves late slots and symbolic references after all identities are bound.
final class ReferenceLinker {
  private ReferenceLinker() {}

  /// Substitutes late-element slots and folds pending [IdReference]s into attributes, now that
  /// every ref on the page is bound.
  static ResolvedNode link(ResolvedNode node, CompilationState state) {
    if (node instanceof ResolvedSlot slot) {
      var content = state.lateSlots.get(slot.index());
      if (content == null) {
        throw new IllegalStateException("Late element was linked before it was laid out");
      }
      return link(content, state);
    }
    if (!(node instanceof ResolvedTag tag)) {
      return node;
    }
    var attrs = tag.attributes();
    if (!tag.idReferences().isEmpty()) {
      var linked = new LinkedHashMap<>(attrs);
      tag.idReferences()
          .forEach(
              (name, reference) -> {
                var ids = new ArrayList<String>(reference.targets().size());
                for (var target : reference.targets()) {
                  ids.add(state.boundDomId(target, "<" + tag.tag().tagName() + " " + name + ">"));
                }
                linked.put(name, reference.prefix() + String.join(" ", ids));
              });
      attrs = Collections.unmodifiableMap(linked);
    }
    var children = new ArrayList<ResolvedNode>(tag.children().size());
    for (var child : tag.children()) {
      children.add(link(child, state));
    }
    var companion = tag.companionSibling();
    return new ResolvedTag(
        tag.tag(),
        attrs,
        Map.of(),
        Collections.unmodifiableList(children),
        companion != null ? (ResolvedTag) link(companion, state) : null);
  }
}
