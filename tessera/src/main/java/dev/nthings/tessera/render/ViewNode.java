package dev.nthings.tessera.render;

import dev.nthings.tessera.core.IdReference;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.render.ResolvedNode.ResolvedRaw;
import dev.nthings.tessera.render.ResolvedNode.ResolvedText;
import java.util.Collections;
import java.util.LinkedHashMap;
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
  /// is emitted as its next sibling.
  record Tag(
      TagName tag,
      CompilationLocation location,
      Map<String, String> attributes,
      Map<String, IdReference> references,
      List<ViewNode> children,
      @Nullable Tag companion)
      implements ViewNode {
    public Tag {
      attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
      references = Collections.unmodifiableMap(new LinkedHashMap<>(references));
      children = List.copyOf(children);
    }

    Tag withAttributes(Map<String, String> attributes) {
      return new Tag(tag, location, attributes, references, children, companion);
    }
  }

  /// Transparent group of siblings that emits no wrapper.
  record Fragment(List<ViewNode> children) implements ViewNode {
    public Fragment {
      children = List.copyOf(children);
    }
  }

  /// Position of a late element, replaced by its content when linked.
  record Slot(int index) implements ViewNode {}

  enum Empty implements ViewNode {
    INSTANCE
  }
}
