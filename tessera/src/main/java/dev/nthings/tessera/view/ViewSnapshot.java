package dev.nthings.tessera.view;

import dev.nthings.tessera.core.ControlProperties;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.EventListener;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.css.StyleSheet;
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
/// - `styleRules`: this render's self-contained embedded rules (`StyleSheet#embeddedRules()`), each
///   with a short key that identifies its text; an embedder may insert each one independently,
///   deduplicate them across roots and keep them, by key, for later renders.
/// - `references`: each element ref bound in this tree and its DOM id, so a backend can find the
///   committed element of a ref.
/// - `ids`: every DOM id the tree renders, generated or explicit, so a backend can detect
///   collisions with other roots.
public record ViewSnapshot(
    List<Node> roots,
    List<StyleSheet.EmbeddedRule> styleRules,
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
  public sealed interface Node permits Tag, Text, Raw, Group, Effect {}

  /// The content of a component, scope, fragment or keyed child, with no element wrapper. A live
  /// backend keeps its complete DOM range together and matches it by `key`, or by `definition`
  /// among unkeyed siblings; document output flattens it. `definition` is the authoring class of a
  /// component or keyed child, the [dev.nthings.tessera.core.Scope#scopeName()] of a scope, or
  /// [dev.nthings.tessera.core.FragmentElement]'s class for a fragment. A scope name is a stable
  /// value, not a scope instance allocated for that render.
  public record Group(@Nullable String key, Object definition, List<Node> children)
      implements Node {
    public Group {
      children = List.copyOf(children);
    }
  }

  /// A linked element. `unresolvedAttributes` names reference attributes whose targets were
  /// reported missing or ambiguous, so validators skip secondary errors for them. `liveProperties`
  /// are the custom properties a live backend sets on the element for its live styles. Only linking
  /// builds one, reusing the expanded element's collections.
  public static final class Tag implements Node {
    private final TagCore core;
    private final Map<String, String> attributes;
    private final Set<String> unresolvedAttributes;
    private final List<Node> children;

    Tag(
        TagCore core,
        Map<String, String> attributes,
        Set<String> unresolvedAttributes,
        List<Node> children) {
      this.core = core;
      this.attributes = TagCore.readOnly(attributes);
      this.unresolvedAttributes = TagCore.readOnly(unresolvedAttributes);
      this.children = TagCore.readOnly(children);
    }

    /// The element's tag.
    public TagName tag() {
      return core.tag();
    }

    /// Where the element was built.
    public Location location() {
      return core.location();
    }

    /// The attributes, reference attributes resolved.
    public Map<String, String> attributes() {
      return attributes;
    }

    /// Reference attributes left out because a target was missing or ambiguous.
    public Set<String> unresolvedAttributes() {
      return unresolvedAttributes;
    }

    /// The bound event handlers, keyed by [EventListener#key()].
    public Map<EventListener.Key, EventListener> events() {
      return core.events();
    }

    /// The controlled form properties.
    public ControlProperties controlProperties() {
      return core.controlProperties();
    }

    /// The custom properties to set on the element for its live styles.
    public Map<String, String> liveProperties() {
      return core.liveProperties();
    }

    /// The linked children.
    public List<Node> children() {
      return children;
    }
  }

  /// Text content, HTML-escaped when emitted.
  public record Text(String content) implements Node, ViewNode {}

  /// An effect a live backend starts after commit and disposes with its position. Its identity is
  /// the key of the [Group] that [dev.nthings.tessera.core.Elements#effect] places it in.
  public record Effect(dev.nthings.tessera.core.Effect effect, Location location)
      implements Node, ViewNode {}

  /// Trusted HTML for document output; live renders reject it.
  public record Raw(String trustedHtml) implements Node, ViewNode {}
}
