package dev.nthings.tessera.view;

import dev.nthings.tessera.core.ControlProperties;
import dev.nthings.tessera.core.EventListener;
import dev.nthings.tessera.core.IdReference;
import dev.nthings.tessera.core.TagName;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Backend-facing: expanded content before linking. Component boundaries, scope boundaries,
/// fragments, late slots and tooltip companions exist only here; [ViewExpansion] links them into
/// [ViewSnapshot] nodes. Text, raw HTML and effects need no linking, so they are the snapshot
/// records themselves.
public sealed interface ViewNode
    permits ViewNode.Tag,
        ViewSnapshot.Text,
        ViewSnapshot.Raw,
        ViewNode.Boundary,
        ViewNode.ScopeBoundary,
        ViewNode.Fragment,
        ViewNode.Slot,
        ViewSnapshot.Effect,
        ViewNode.Empty {

  /// The content of a component or keyed child, emitted without a wrapper. A scope looks through
  /// this wrapper for its element host. `definition` identifies it among unkeyed siblings, as
  /// [ViewSnapshot.Group#definition()] describes, and `key` is the sibling key, if any.
  record Boundary(
      @Nullable String key, Object definition, Location location, List<ViewNode> children)
      implements ViewNode {
    public Boundary {
      children = List.copyOf(children);
    }
  }

  /// A scope's own sibling namespace, emitted without a wrapper. An enclosing scope does not look
  /// through it or replace its host. `definition` is [dev.nthings.tessera.core.Scope#scopeName()],
  /// stable across renders, and `key` is the sibling key when the scope itself is keyed.
  record ScopeBoundary(
      @Nullable String key, String definition, Location location, List<ViewNode> children)
      implements ViewNode {
    public ScopeBoundary {
      children = List.copyOf(children);
    }
  }

  /// The children of a fragment, emitted without a wrapper; `key` is the sibling key, if any.
  /// Unlike a [Boundary], a scope never looks through a fragment for its root element.
  record Fragment(@Nullable String key, Location location, List<ViewNode> children)
      implements ViewNode {
    public Fragment {
      children = List.copyOf(children);
    }
  }

  /// An expanded element. Only expansion builds one, handing over collections it no longer touches,
  /// so linking reuses them without copying.
  final class Tag implements ViewNode {
    private final TagCore core;
    private final Map<String, String> attributes;
    private final Map<String, IdReference> references;
    private final List<ViewNode> children;
    private final @Nullable Tag companion;

    Tag(
        TagCore core,
        Map<String, String> attributes,
        Map<String, IdReference> references,
        List<ViewNode> children,
        @Nullable Tag companion) {
      this.core = core;
      this.attributes = TagCore.readOnly(attributes);
      this.references = TagCore.readOnly(references);
      this.children = TagCore.readOnly(children);
      this.companion = companion;
    }

    /// The element's tag.
    public TagName tag() {
      return core.tag();
    }

    /// Where the element was built.
    public Location location() {
      return core.location();
    }

    /// The attributes, before reference attributes are resolved.
    public Map<String, String> attributes() {
      return attributes;
    }

    /// Reference attributes, resolved to DOM ids when linked.
    public Map<String, IdReference> references() {
      return references;
    }

    /// The bound event handlers, keyed by [EventListener#key()].
    public Map<EventListener.Key, EventListener> events() {
      return core.events();
    }

    /// The controlled form properties.
    public ControlProperties controlProperties() {
      return core.controlProperties();
    }

    /// The custom properties a live backend sets on the element for its live styles.
    public Map<String, String> liveProperties() {
      return core.liveProperties();
    }

    /// The expanded children.
    public List<ViewNode> children() {
      return children;
    }

    /// The tooltip emitted as this element's next sibling, if any.
    public @Nullable Tag companion() {
      return companion;
    }

    TagCore core() {
      return core;
    }

    Tag withAttributes(Map<String, String> attributes) {
      return new Tag(core, attributes, references, children, companion);
    }
  }

  /// Placeholder for a late element, filled by the backend that placed it.
  final class Slot implements ViewNode {
    private @Nullable ViewNode content;

    /// Fills the slot with its late content, once the main flow is expanded.
    public void fill(ViewNode content) {
      this.content = content;
    }

    ViewNode content() {
      var filled = content;
      if (filled == null) {
        throw new IllegalStateException("Late element was linked before it was laid out");
      }
      return filled;
    }
  }

  /// Content that emits nothing.
  enum Empty implements ViewNode {
    INSTANCE
  }
}
