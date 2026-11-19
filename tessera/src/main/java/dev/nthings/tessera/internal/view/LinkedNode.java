package dev.nthings.tessera.internal.view;

import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.TagName;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// Linked view nodes shared by static compilation and live rendering. Linking has resolved element
/// references, late slots, control defaults and tooltip companions, so every node is ready for
/// validation, HTML emission or snapshot projection.
public sealed interface LinkedNode<M>
    permits LinkedNode.Tag, LinkedNode.Text, LinkedNode.Group, LinkedNode.Special {

  /// The content of a component, scope, fragment or keyed child, with no element wrapper. A live
  /// backend keeps its complete DOM range together and matches it by `key`, or by `definition`
  /// among unkeyed siblings; document output flattens it.
  record Group<M>(@Nullable String key, Object definition, List<LinkedNode<M>> children)
      implements LinkedNode<M> {
    public Group {
      children = List.copyOf(children);
    }
  }

  /// The parts of a native control's markup supplied by typed initial declarations.
  record Defaults(boolean value, boolean checked, boolean text, boolean selection) {
    public static final Defaults NONE = new Defaults(false, false, false, false);
  }

  /// A linked element. `unresolvedAttributes` names reference attributes whose targets were
  /// reported missing or ambiguous, so validators skip secondary errors for them. `liveProperties`
  /// are the custom properties a live backend sets on the element for its live styles.
  final class Tag<M> implements LinkedNode<M> {
    private final TagName tag;
    private final Location location;
    private final @Nullable M payload;
    private final Map<String, String> liveProperties;
    private final Map<String, String> attributes;
    private final Set<String> unresolvedAttributes;
    private final List<LinkedNode<M>> children;
    private final Defaults defaults;

    Tag(
        TagName tag,
        Location location,
        @Nullable M payload,
        Map<String, String> liveProperties,
        Map<String, String> attributes,
        Set<String> unresolvedAttributes,
        List<LinkedNode<M>> children,
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
    Set<String> unresolvedAttributes() {
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
    Tag<M> withControlMarkup(
        Map<String, String> attributes, List<LinkedNode<M>> children, Defaults defaults) {
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
    public List<LinkedNode<M>> children() {
      return children;
    }
  }

  /// Text content, HTML-escaped when emitted.
  record Text<M>(String content) implements LinkedNode<M> {}

  /// A backend-owned leaf. The structural passes only need to know if it supplies readable text.
  record Special<M>(M payload, boolean readable) implements LinkedNode<M> {}
}
