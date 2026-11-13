package dev.nthings.tessera.compiler;

import dev.nthings.tessera.compiler.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.compiler.ResolvedNode.ResolvedTag;
import dev.nthings.tessera.compiler.ResolvedNode.ResolvedText;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// Turns expanded [ExpandedNode]s into [ResolvedNode]s once every identity on the page is bound.
final class ReferenceLinker {
  private ReferenceLinker() {}

  /// Substitutes late slots, splices fragments, places tooltip companions after their invoker and
  /// folds [IdReference]s into attributes. Sibling keys are checked after late slots resolve.
  static List<ResolvedNode> link(List<ExpandedNode> nodes, ExpansionState state) {
    var out = new ArrayList<ResolvedNode>(nodes.size());
    var siblings = new Siblings(state, out);
    nodes.forEach(siblings::link);
    return out;
  }

  /// One authored sibling namespace. Component boundaries, fragments and keyed children keep
  /// their own child namespaces even though document output splices their content into `out`.
  private static final class Siblings {
    private final ExpansionState state;
    private final List<ResolvedNode> out;
    private @Nullable Set<String> keys;

    private Siblings(ExpansionState state, List<ResolvedNode> out) {
      this.state = state;
      this.out = out;
    }

    private void link(ExpandedNode node) {
      switch (node) {
        case ExpandedNode.Slot slot -> link(slot.content());
        case ExpandedNode.Boundary(var content) -> new Siblings(state, out).link(content);
        case ExpandedNode.ScopeBoundary(var content) -> new Siblings(state, out).link(content);
        case ExpandedNode.Fragment fragment -> {
          var children = new Siblings(state, out);
          fragment.children().forEach(children::link);
        }
        case ExpandedNode.Keyed(var key, var location, var content) -> {
          var claimed = keys;
          if (claimed == null) {
            claimed = new HashSet<>();
            keys = claimed;
          }
          if (!claimed.add(key)) {
            state.report(
                location,
                "duplicate-key",
                "Duplicate sibling key '" + key + "'",
                "Give each child of this parent a distinct key.");
          }
          new Siblings(state, out).link(content);
        }
        case ExpandedNode.Empty _ -> {}
        case ResolvedText text -> out.add(text);
        case ResolvedRaw raw -> out.add(raw);
        case ExpandedNode.Tag tag -> {
          out.add(linkTag(tag, state));
          var companion = tag.companion();
          if (companion != null) {
            link(companion);
          }
        }
      }
    }
  }

  private static ResolvedTag linkTag(ExpandedNode.Tag tag, ExpansionState state) {
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
    var children = link(tag.children(), state);
    return new ResolvedTag(tag.tag(), tag.location(), attrs, unresolved, children);
  }
}
