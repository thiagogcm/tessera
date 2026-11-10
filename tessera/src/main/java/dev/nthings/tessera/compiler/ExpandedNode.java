package dev.nthings.tessera.compiler;

import dev.nthings.tessera.compiler.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.compiler.ResolvedNode.ResolvedText;
import dev.nthings.tessera.core.IdReference;
import dev.nthings.tessera.core.TagName;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Expanded content before linking. Component boundaries, late slots, fragments and tooltip
/// companions exist only here, along with sibling keys; [ReferenceLinker] resolves them, so
/// validation and HTML emit see [ResolvedNode]s alone.
/// Text and raw HTML are already resolved.
sealed interface ExpandedNode
    permits ExpandedNode.Tag,
        ResolvedText,
        ResolvedRaw,
        ExpandedNode.Fragment,
        ExpandedNode.Boundary,
        ExpandedNode.Keyed,
        ExpandedNode.Slot,
        ExpandedNode.Empty {

  /// An expanded element; `companion` is emitted as its next sibling. Takes ownership of the
  /// maps and list it is given.
  record Tag(
      TagName tag,
      Location location,
      Map<String, String> attributes,
      Map<String, IdReference> references,
      List<ExpandedNode> children,
      @Nullable Tag companion)
      implements ExpandedNode {
    Tag withAttributes(Map<String, String> attributes) {
      return new Tag(tag, location, attributes, references, children, companion);
    }
  }

  /// Siblings emitted without a wrapper.
  record Fragment(List<ExpandedNode> children) implements ExpandedNode {}

  /// A component or scope's own sibling namespace. Scopes look through this wrapper for their
  /// root element; linking removes it without emitting an HTML wrapper.
  record Boundary(ExpandedNode content) implements ExpandedNode {}

  /// A keyed child's identity, retained until linking can check its resolved siblings. The
  /// wrapper emits no HTML and keeps its content's own sibling namespace separate.
  record Keyed(String key, Location location, ExpandedNode content) implements ExpandedNode {}

  /// Placeholder for a late element, filled after the main flow is expanded.
  final class Slot implements ExpandedNode {
    private @Nullable ExpandedNode content;

    void fill(ExpandedNode content) {
      this.content = content;
    }

    ExpandedNode content() {
      var filled = content;
      if (filled == null) {
        throw new IllegalStateException("Late element was linked before it was laid out");
      }
      return filled;
    }
  }

  enum Empty implements ExpandedNode {
    INSTANCE
  }
}
