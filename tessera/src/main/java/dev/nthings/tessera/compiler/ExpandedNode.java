package dev.nthings.tessera.compiler;

import dev.nthings.tessera.compiler.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.compiler.ResolvedNode.ResolvedText;
import dev.nthings.tessera.core.IdReference;
import dev.nthings.tessera.core.TagName;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Expanded content before linking. Late slots, fragments and tooltip companions exist only
/// here; [ReferenceLinker] resolves them, so validation and HTML emit see [ResolvedNode]s alone.
/// Text and raw HTML are already resolved.
sealed interface ExpandedNode
    permits ExpandedNode.Tag,
        ResolvedText,
        ResolvedRaw,
        ExpandedNode.Fragment,
        ExpandedNode.Slot,
        ExpandedNode.Empty {

  /// An expanded element; `companion` is emitted as its next sibling. Takes ownership of the
  /// maps and list it is given.
  record Tag(
      TagName tag,
      NodeLocation location,
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
