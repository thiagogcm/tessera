package dev.nthings.tessera.internal.view;

import dev.nthings.tessera.core.FragmentElement;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.view.ViewSnapshot;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// Turns expanded `ViewNode<M>`s into [ViewSnapshot] nodes once every identity of the expansion is
/// bound, checking sibling keys on the way. Component boundaries, scope boundaries and fragments
/// become [ViewSnapshot.Group]s, each its own key namespace. A scope group keeps the scope name
/// passed in as its definition.
final class ReferenceLinker<M> {
  private final ViewState state;

  ReferenceLinker(ViewState state) {
    this.state = state;
  }

  /// Substitutes late slots, places tooltip companions after their invoker and folds
  /// [dev.nthings.tessera.internal.authoring.IdReference]s into attributes.
  List<ViewSnapshot.Node<M>> link(ViewNode<M> node) {
    return linkChildren(List.of(node));
  }

  private List<ViewSnapshot.Node<M>> linkChildren(List<ViewNode<M>> nodes) {
    if (nodes.isEmpty()) {
      return List.of();
    }
    var siblings = new Siblings(new ArrayList<>(nodes.size()));
    nodes.forEach(siblings::link);
    return siblings.out;
  }

  /// One sibling list being linked, with its key namespace.
  private final class Siblings {
    private final List<ViewSnapshot.Node<M>> out;

    /// Created at the first key: most sibling lists have none.
    private @Nullable Set<String> keys;

    private Siblings(List<ViewSnapshot.Node<M>> out) {
      this.out = out;
    }

    private void link(ViewNode<M> node) {
      switch (node) {
        case ViewNode.Slot<M> slot -> link(slot.content());
        case ViewNode.Empty<M> _ -> {}
        case ViewNode.Special<M> special ->
            out.add(new ViewSnapshot.Special<>(special.payload(), special.readable()));
        case ViewNode.Text<M> text -> out.add(new ViewSnapshot.Text<>(text.content()));
        case ViewNode.Boundary<M> boundary ->
            group(boundary.key(), boundary.definition(), boundary.location(), boundary.children());
        case ViewNode.ScopeBoundary<M> boundary ->
            group(boundary.key(), boundary.definition(), boundary.location(), boundary.children());
        case ViewNode.Fragment<M> fragment ->
            group(fragment.key(), FragmentElement.class, fragment.location(), fragment.children());
        case ViewNode.Tag<M> tag -> {
          out.add(linkTag(tag));
          var companion = tag.companion();
          if (companion != null) {
            link(companion);
          }
        }
      }
    }

    private void group(
        @Nullable String key, Object definition, Location location, List<ViewNode<M>> children) {
      if (key != null && !claim(key)) {
        state.report(
            location,
            "duplicate-key",
            "Duplicate sibling key '" + key + "'",
            "Give each child in this parent a distinct stable key.");
      }
      out.add(new ViewSnapshot.Group<>(key, definition, linkChildren(children)));
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

  private ViewSnapshot.Tag<M> linkTag(ViewNode.Tag<M> tag) {
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
              var value = String.join(" ", ids);
              linked.put(
                  name,
                  reference.prefix().equals("#")
                      ? "#" + fragment(value)
                      : reference.prefix() + value);
            } else {
              linked.remove(name);
              missing.add(name);
            }
          });
      attrs = linked;
      unresolved = missing;
    }
    var core = tag.core();
    var linked =
        new ViewSnapshot.Tag<>(
            core.tag(),
            core.location(),
            core.payload(),
            core.liveProperties(),
            attrs,
            unresolved,
            linkChildren(tag.children()),
            ViewSnapshot.Defaults.NONE);
    return ControlLowerer.lower(linked, core.initial(), state::report);
  }

  /// `id` as a URL fragment: characters a fragment may not hold, `%` and non-ASCII characters are
  /// percent-encoded as UTF-8, which browsers decode before matching the id.
  private static String fragment(String id) {
    var sb = new StringBuilder(id.length());
    for (var b : id.getBytes(StandardCharsets.UTF_8)) {
      var c = (char) (b & 0xFF);
      if ((c >= 'a' && c <= 'z')
          || (c >= 'A' && c <= 'Z')
          || (c >= '0' && c <= '9')
          || FRAGMENT_SAFE.indexOf(c) >= 0) {
        sb.append(c);
      } else {
        sb.append('%').append(HEX.charAt(c >> 4)).append(HEX.charAt(c & 0xF));
      }
    }
    return sb.toString();
  }

  private static final String FRAGMENT_SAFE = "-._~!$&'()*+,;=:@/?";
  private static final String HEX = "0123456789ABCDEF";
}
