package dev.nthings.tessera.render;

import dev.nthings.tessera.core.IdReference;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.render.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.render.ResolvedNode.ResolvedText;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Expanded page content before linking. Late slots, fragments and tooltip companions exist only
/// here; [ReferenceLinker] resolves them, so validation and painting see [ResolvedNode]s alone.
/// Text and raw HTML need no linking and are the resolved records themselves.
sealed interface ViewNode
    permits ViewNode.Tag,
        ResolvedText,
        ResolvedRaw,
        ViewNode.Fragment,
        ViewNode.Slot,
        ViewNode.Empty {

  /// An expanded element; `references` are folded into attributes when linked, and `companion`
  /// is emitted as its next sibling. Takes ownership of the maps and list it is given.
  record Tag(
      TagName tag,
      ViewLocation location,
      Map<String, String> attributes,
      Map<String, IdReference> references,
      List<ViewNode> children,
      @Nullable Tag companion)
      implements ViewNode {
    public Tag {
      attributes = Collections.unmodifiableMap(attributes);
      references = Collections.unmodifiableMap(references);
      children = Collections.unmodifiableList(children);
    }

    Tag withAttributes(Map<String, String> attributes) {
      return new Tag(tag, location, attributes, references, children, companion);
    }
  }

  /// Transparent group of siblings that emits no wrapper.
  record Fragment(List<ViewNode> children) implements ViewNode {
    public Fragment {
      children = Collections.unmodifiableList(children);
    }
  }

  /// Position of a late element, filled with its content after the main flow is expanded.
  final class Slot implements ViewNode {
    private @Nullable ViewNode content;

    void fill(ViewNode content) {
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

  enum Empty implements ViewNode {
    INSTANCE
  }
}
