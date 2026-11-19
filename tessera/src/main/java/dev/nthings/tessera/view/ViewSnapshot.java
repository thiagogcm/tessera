package dev.nthings.tessera.view;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.internal.view.LinkedNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

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
    List<Node<M>> roots,
    List<EmbeddedRule> styleRules,
    Map<ElementRef, String> references,
    Set<String> ids) {

  public ViewSnapshot {
    roots = List.copyOf(roots);
    styleRules = List.copyOf(styleRules);
    // Expansion hands over its identity maps; a render avoids copying them every turn.
    references = Collections.unmodifiableMap(references);
    ids = Collections.unmodifiableSet(ids);
  }

  /// A linked node.
  public sealed interface Node<M> permits Tag, Text, Group, Special {}

  /// The content of a component, scope, fragment or keyed child, with no element wrapper. A live
  /// backend keeps its complete DOM range together and matches it by `key`, or by `definition`
  /// among unkeyed siblings. `definition` is the authoring class of a component or keyed child, the
  /// [dev.nthings.tessera.core.Scope#scopeName()] of a scope, or
  /// [dev.nthings.tessera.core.FragmentElement]'s class for a fragment.
  public record Group<M>(@Nullable String key, Object definition, List<Node<M>> children)
      implements Node<M> {
    public Group {
      children = List.copyOf(children);
    }
  }

  /// The parts of a native control's markup supplied by typed initial declarations.
  public record Defaults(boolean value, boolean checked, boolean text, boolean selection) {
    public static final Defaults NONE = new Defaults(false, false, false, false);
  }

  /// A linked element ready for DOM reconciliation. `liveProperties` are the custom properties a
  /// live backend sets on the element for its live styles.
  public static final class Tag<M> implements Node<M> {
    private final TagName tag;
    private final Location location;
    private final @Nullable M payload;
    private final Map<String, String> liveProperties;
    private final Map<String, String> attributes;
    private final List<Node<M>> children;
    private final Defaults defaults;

    Tag(
        TagName tag,
        Location location,
        @Nullable M payload,
        Map<String, String> liveProperties,
        Map<String, String> attributes,
        List<Node<M>> children,
        Defaults defaults) {
      this.tag = tag;
      this.location = location;
      this.payload = payload;
      this.liveProperties = readOnly(liveProperties);
      this.attributes = readOnly(attributes);
      this.children = readOnly(children);
      this.defaults = defaults;
    }

    private static <K, V> Map<K, V> readOnly(Map<K, V> map) {
      return map.isEmpty() ? Map.of() : Collections.unmodifiableMap(map);
    }

    private static <T> List<T> readOnly(List<T> list) {
      return list.isEmpty() ? List.of() : Collections.unmodifiableList(list);
    }

    /// The element's tag.
    public TagName tag() {
      return tag;
    }

    /// Where the element was built.
    public Location location() {
      return location;
    }

    /// The attributes, reference attributes resolved.
    public Map<String, String> attributes() {
      return attributes;
    }

    /// Metadata attached by the backend, such as a browser host's bindings.
    public @Nullable M payload() {
      return payload;
    }

    /// The custom properties to set on the element for its live styles.
    public Map<String, String> liveProperties() {
      return liveProperties;
    }

    /// The control markup that is applied only when the element is inserted.
    public Defaults defaults() {
      return defaults;
    }

    /// The linked children.
    public List<Node<M>> children() {
      return children;
    }
  }

  /// Text content, HTML-escaped when emitted.
  public record Text<M>(String content) implements Node<M> {}

  /// A backend-owned leaf. The structural passes only need to know if it supplies readable text.
  public record Special<M>(M payload, boolean readable) implements Node<M> {}

  static <M> List<Node<M>> project(List<LinkedNode<M>> nodes) {
    if (nodes.isEmpty()) {
      return List.of();
    }
    var projected = new ArrayList<Node<M>>(nodes.size());
    for (var node : nodes) {
      projected.add(
          switch (node) {
            case LinkedNode.Text<M> text -> new Text<>(text.content());
            case LinkedNode.Special<M> special ->
                new Special<>(special.payload(), special.readable());
            case LinkedNode.Group<M> group ->
                new Group<>(group.key(), group.definition(), project(group.children()));
            case LinkedNode.Tag<M> tag ->
                new Tag<>(
                    tag.tag(),
                    tag.location(),
                    tag.payload(),
                    tag.liveProperties(),
                    tag.attributes(),
                    project(tag.children()),
                    projectDefaults(tag.defaults()));
          });
    }
    return projected;
  }

  private static Defaults projectDefaults(LinkedNode.Defaults defaults) {
    if (defaults == LinkedNode.Defaults.NONE) {
      return Defaults.NONE;
    }
    return new Defaults(
        defaults.value(), defaults.checked(), defaults.text(), defaults.selection());
  }
}
