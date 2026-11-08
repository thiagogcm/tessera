package dev.nthings.tessera.render;

import dev.nthings.tessera.render.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.render.ResolvedNode.ResolvedTag;
import dev.nthings.tessera.render.ResolvedNode.ResolvedText;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/// Turns expanded [ViewNode]s into [ResolvedNode]s once every identity on the page is bound.
final class ReferenceLinker {
  private ReferenceLinker() {}

  /// Substitutes late slots, splices fragments, places tooltip companions after their invoker and
  /// folds [IdReference]s into attributes; appends to `out`.
  static void link(ViewNode node, CompilationState state, List<ResolvedNode> out) {
    switch (node) {
      case ViewNode.Slot slot -> link(slot.content(), state, out);
      case ViewNode.Fragment fragment -> {
        for (var child : fragment.children()) {
          link(child, state, out);
        }
      }
      case ViewNode.Empty _ -> {}
      case ResolvedText text -> out.add(text);
      case ResolvedRaw raw -> out.add(raw);
      case ViewNode.Tag tag -> {
        out.add(linkTag(tag, state));
        var companion = tag.companion();
        if (companion != null) {
          link(companion, state, out);
        }
      }
    }
  }

  private static ResolvedTag linkTag(ViewNode.Tag tag, CompilationState state) {
    var attrs = tag.attributes();
    Set<String> unresolved = Set.of();
    if (!tag.references().isEmpty()) {
      var linked = new LinkedHashMap<>(attrs);
      var missing = new LinkedHashSet<String>();
      tag.references()
          .forEach(
              (name, reference) -> {
                var ids = new ArrayList<String>(reference.targets().size());
                for (var target : reference.targets()) {
                  var id =
                      state.boundId(
                          target, "<" + tag.tag().tagName() + " " + name + ">", tag.location());
                  if (id != null) {
                    ids.add(id);
                  }
                }
                // Unresolved attributes cannot be validated; other nodes still can.
                if (ids.size() == reference.targets().size()) {
                  linked.put(name, reference.prefix() + String.join(" ", ids));
                } else {
                  linked.remove(name);
                  missing.add(name);
                }
              });
      attrs = linked;
      unresolved = missing;
    }
    var children = new ArrayList<ResolvedNode>(tag.children().size());
    for (var child : tag.children()) {
      link(child, state, children);
    }
    return new ResolvedTag(tag.tag(), tag.location(), attrs, unresolved, children);
  }
}
