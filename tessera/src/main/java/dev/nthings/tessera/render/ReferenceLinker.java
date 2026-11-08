package dev.nthings.tessera.render;

import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IdReference;
import dev.nthings.tessera.render.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.render.ResolvedNode.ResolvedTag;
import dev.nthings.tessera.render.ResolvedNode.ResolvedText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/// Turns expanded [ViewNode]s into [ResolvedNode]s once every identity on the page is bound.
final class ReferenceLinker {
  private final ViewExpansion expansion;
  private final CompilationState state;

  ReferenceLinker(ViewExpansion expansion, CompilationState state) {
    this.expansion = expansion;
    this.state = state;
  }

  /// Substitutes late slots, splices fragments into their parent, places tooltip companions after
  /// their invoker and folds pending [IdReference]s into attributes, appending the result to
  /// `out`.
  void link(ViewNode node, List<ResolvedNode> out) {
    switch (node) {
      case ViewNode.Slot slot -> link(expansion.resolveSlot(slot.index()), out);
      case ViewNode.Fragment fragment -> fragment.children().forEach(child -> link(child, out));
      case ViewNode.Empty _ -> {}
      case ResolvedText text -> out.add(text);
      case ResolvedRaw raw -> out.add(raw);
      case ViewNode.Tag tag -> {
        out.add(linkTag(tag));
        var companion = tag.companion();
        if (companion != null) {
          link(companion, out);
        }
      }
    }
  }

  private ResolvedTag linkTag(ViewNode.Tag tag) {
    var attrs = tag.attributes();
    var unresolved = new LinkedHashMap<String, IdReference>();
    if (!tag.references().isEmpty()) {
      var linked = new LinkedHashMap<>(attrs);
      tag.references()
          .forEach(
              (name, reference) -> {
                var ids = new ArrayList<String>(reference.targets().size());
                for (var target : reference.targets()) {
                  var id =
                      expansion.boundId(
                          target, "<" + tag.tag().tagName() + " " + name + ">", tag.location());
                  if (id != null) {
                    ids.add(id);
                  }
                }
                // An unresolved attribute cannot be validated meaningfully. Other nodes still can.
                if (ids.size() == reference.targets().size()) {
                  linked.put(name, reference.prefix() + String.join(" ", ids));
                } else {
                  linked.remove(name);
                  unresolved.put(name, reference);
                }
              });
      attrs = Collections.unmodifiableMap(linked);
    }
    var children = new ArrayList<ResolvedNode>(tag.children().size());
    tag.children().forEach(child -> link(child, children));
    var resolved =
        new ResolvedTag(
            tag.tag(),
            tag.location(),
            attrs,
            Collections.unmodifiableMap(unresolved),
            Collections.unmodifiableList(children));
    var id = attrs.get("id");
    if (id != null && !id.isBlank()) {
      state.elementsById.putIfAbsent(id, resolved);
    }
    var labelled = attrs.get("for");
    if (tag.tag() == HtmlTag.LABEL && labelled != null) {
      state.labelledControlIds.add(labelled);
    }
    return resolved;
  }
}
