package dev.nthings.tessera.view;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.TagName;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// Backend-facing: the linked view tree of one live render ([ViewRenderer]), with its generated
/// CSS. Linking has resolved element references, late slots and tooltip companions, so every node
/// is final output; a document backend links the same [Node]s without a snapshot.
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
  /// among unkeyed siblings; document output flattens it. `definition` is the authoring class of a
  /// component or keyed child, the [dev.nthings.tessera.core.Scope#scopeName()] of a scope, or
  /// [dev.nthings.tessera.core.FragmentElement]'s class for a fragment. A scope name is a stable
  /// value, not a scope instance allocated for that render.
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

  /// A linked element. `unresolvedAttributes` names reference attributes whose targets were
  /// reported missing or ambiguous, so validators skip secondary errors for them. `liveProperties`
  /// are the custom properties a live backend sets on the element for its live styles.
  public static final class Tag<M> implements Node<M> {
    private final TagName tag;
    private final Location location;
    private final @Nullable M payload;
    private final Map<String, String> liveProperties;
    private final Map<String, String> attributes;
    private final Set<String> unresolvedAttributes;
    private final List<Node<M>> children;
    private final Defaults defaults;

    public Tag(
        TagName tag,
        Location location,
        @Nullable M payload,
        Map<String, String> liveProperties,
        Map<String, String> attributes,
        Set<String> unresolvedAttributes,
        List<Node<M>> children,
        Defaults defaults) {
      this.tag = tag;
      this.location = location;
      this.payload = payload;
      this.liveProperties = readOnly(liveProperties);
      this.attributes = readOnly(attributes);
      this.unresolvedAttributes = readOnly(unresolvedAttributes);
      this.children = readOnly(children);
      this.defaults = defaults;
    }

    private static <K, V> Map<K, V> readOnly(Map<K, V> map) {
      return map.isEmpty() ? Map.of() : Collections.unmodifiableMap(map);
    }

    private static <T> List<T> readOnly(List<T> list) {
      return list.isEmpty() ? List.of() : Collections.unmodifiableList(list);
    }

    private static <T> Set<T> readOnly(Set<T> set) {
      return set.isEmpty() ? Set.of() : Collections.unmodifiableSet(set);
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

    /// Reference attributes left out because a target was missing or ambiguous.
    public Set<String> unresolvedAttributes() {
      return unresolvedAttributes;
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

    /// Copies this tag with lowered control markup.
    public Tag<M> withControlMarkup(
        Map<String, String> attributes, List<Node<M>> children, Defaults defaults) {
      return new Tag<>(
          tag,
          location,
          payload,
          liveProperties,
          attributes,
          unresolvedAttributes,
          children,
          defaults);
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
}
