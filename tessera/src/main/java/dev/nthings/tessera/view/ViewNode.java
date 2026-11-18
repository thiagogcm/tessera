package dev.nthings.tessera.view;

import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.internal.authoring.IdReference;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Backend-facing: expanded content before linking. Component boundaries, scope boundaries,
/// fragments, late slots and tooltip companions exist only here; [ViewExpansion] links them into
/// [ViewSnapshot] nodes. Text and backend-specific leaves need no linking and share their snapshot
/// records.
public sealed interface ViewNode<M>
    permits ViewNode.Tag,
        ViewSnapshot.Text,
        ViewNode.Boundary,
        ViewNode.ScopeBoundary,
        ViewNode.Fragment,
        ViewNode.Slot,
        ViewSnapshot.Special,
        ViewNode.Empty {

  /// The content of a component or keyed child, emitted without a wrapper. A scope looks through
  /// this wrapper for its element host. `definition` identifies it among unkeyed siblings, as
  /// [ViewSnapshot.Group#definition()] describes, and `key` is the sibling key, if any.
  record Boundary<M>(
      @Nullable String key, Object definition, Location location, List<ViewNode<M>> children)
      implements ViewNode<M> {
    public Boundary {
      children = List.copyOf(children);
    }
  }

  /// A scope's own sibling namespace, emitted without a wrapper. An enclosing scope does not look
  /// through it or replace its host. `definition` is [dev.nthings.tessera.core.Scope#scopeName()],
  /// stable across renders, and `key` is the sibling key when the scope itself is keyed.
  record ScopeBoundary<M>(
      @Nullable String key, String definition, Location location, List<ViewNode<M>> children)
      implements ViewNode<M> {
    public ScopeBoundary {
      children = List.copyOf(children);
    }
  }

  /// The children of a fragment, emitted without a wrapper; `key` is the sibling key, if any.
  /// Unlike a [Boundary], a scope never looks through a fragment for its root element.
  record Fragment<M>(@Nullable String key, Location location, List<ViewNode<M>> children)
      implements ViewNode<M> {
    public Fragment {
      children = List.copyOf(children);
    }
  }

  /// An expanded element. Only expansion builds one, handing over collections it no longer touches,
  /// so linking reuses them without copying.
  final class Tag<M> implements ViewNode<M> {
    private final TagCore<M> core;
    private final Map<String, String> attributes;
    private final Map<String, IdReference> references;
    private final List<ViewNode<M>> children;
    private final @Nullable Tag<M> companion;

    Tag(
        TagCore<M> core,
        Map<String, String> attributes,
        Map<String, IdReference> references,
        List<ViewNode<M>> children,
        @Nullable Tag<M> companion) {
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

    /// The expanded children.
    public List<ViewNode<M>> children() {
      return children;
    }

    /// The tooltip emitted as this element's next sibling, if any.
    public @Nullable Tag<M> companion() {
      return companion;
    }

    TagCore<M> core() {
      return core;
    }

    Tag<M> withAttributes(Map<String, String> attributes) {
      return new Tag<>(core, attributes, references, children, companion);
    }
  }

  /// Placeholder for a late element, filled by the backend that placed it.
  final class Slot<M> implements ViewNode<M> {
    private @Nullable ViewNode<M> content;

    /// Fills the slot with its late content, once the main flow is expanded.
    public void fill(ViewNode<M> content) {
      this.content = content;
    }

    ViewNode<M> content() {
      var filled = content;
      if (filled == null) {
        throw new IllegalStateException("Late element was linked before it was laid out");
      }
      return filled;
    }
  }

  /// Content that emits nothing.
  record Empty<M>() implements ViewNode<M> {}
}
