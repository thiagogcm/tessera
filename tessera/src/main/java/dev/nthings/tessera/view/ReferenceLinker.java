package dev.nthings.tessera.view;

import dev.nthings.tessera.core.FragmentElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// Turns expanded [ViewNode]s into [ViewSnapshot] nodes once every identity of the expansion is
/// bound, checking sibling keys on the way. Component boundaries, scope boundaries and fragments
/// become [ViewSnapshot.Group]s, each its own key namespace. A scope group keeps the scope name
/// passed in as its definition.
final class ReferenceLinker {
  private final ViewState state;

  ReferenceLinker(ViewState state) {
    this.state = state;
  }

  /// Substitutes late slots, places tooltip companions after their invoker and folds
  /// [dev.nthings.tessera.core.IdReference]s into attributes.
  List<ViewSnapshot.Node> link(ViewNode node) {
    return linkChildren(List.of(node));
  }

  private List<ViewSnapshot.Node> linkChildren(List<ViewNode> nodes) {
    if (nodes.isEmpty()) {
      return List.of();
    }
    var siblings = new Siblings(new ArrayList<>(nodes.size()));
    nodes.forEach(siblings::link);
    return siblings.out;
  }

  /// One sibling list being linked, with its key namespace.
  private final class Siblings {
    private final List<ViewSnapshot.Node> out;

    /// Created at the first key: most sibling lists have none.
    private @Nullable Set<String> keys;

    private Siblings(List<ViewSnapshot.Node> out) {
      this.out = out;
    }

    private void link(ViewNode node) {
      switch (node) {
        case ViewNode.Slot slot -> link(slot.content());
        case ViewNode.Empty _ -> {}
        case ViewSnapshot.Effect effect -> out.add(effect);
        case ViewSnapshot.Text text -> out.add(text);
        case ViewSnapshot.Raw raw -> out.add(raw);
        case ViewNode.Boundary boundary ->
            group(boundary.key(), boundary.definition(), boundary.location(), boundary.children());
        case ViewNode.ScopeBoundary boundary ->
            group(boundary.key(), boundary.definition(), boundary.location(), boundary.children());
        case ViewNode.Fragment fragment ->
            group(fragment.key(), FragmentElement.class, fragment.location(), fragment.children());
        case ViewNode.Tag tag -> {
          out.add(linkTag(tag));
          var companion = tag.companion();
          if (companion != null) {
            link(companion);
          }
        }
      }
    }

    private void group(
        @Nullable String key, Object definition, Location location, List<ViewNode> children) {
      if (key != null && !claim(key)) {
        state.report(
            location,
            "duplicate-key",
            "Duplicate sibling key '" + key + "'",
            "Give each child in this parent a distinct stable key.");
      }
      out.add(new ViewSnapshot.Group(key, definition, linkChildren(children)));
    }

    /// Whether `key` is new among these siblings.
    private boolean claim(String key) {
      var claimed = keys;
      if (claimed == null) {
        claimed = new HashSet<>();
        keys = claimed;
      }
      return claimed.add(key);
    }
  }

  private ViewSnapshot.Tag linkTag(ViewNode.Tag tag) {
    var attrs = tag.attributes();
    Set<String> unresolved = Set.of();
    if (!tag.references().isEmpty()) {
      var linked = new LinkedHashMap<>(attrs);
      var missing = new LinkedHashSet<String>();
      tag.references()
          .forEach((name, reference) -> {
            var ids = new ArrayList<String>(reference.targets().size());
            var usage = "<" + tag.tag().tagName() + " " + name + ">";
            for (var target : reference.targets()) {
              var id = state.boundId(target, usage, tag.location());
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
    return new ViewSnapshot.Tag(tag.core(), attrs, unresolved, linkChildren(tag.children()));
  }
}
