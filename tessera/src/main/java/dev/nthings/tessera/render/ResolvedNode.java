package dev.nthings.tessera.render;

import dev.nthings.tessera.core.IdReference;
import dev.nthings.tessera.core.TagName;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

sealed interface ResolvedNode
    permits ResolvedNode.ResolvedTag,
        ResolvedNode.ResolvedFragment,
        ResolvedNode.ResolvedText,
        ResolvedNode.ResolvedRaw,
        ResolvedNode.ResolvedEmpty,
        ResolvedNode.ResolvedSlot {

  /// Resolved element; `idReferences` are pending until the prepaint link pass folds them into
  /// `attributes`.
  record ResolvedTag(
      TagName tag,
      CompilationLocation location,
      Map<String, String> attributes,
      Map<String, IdReference> idReferences,
      List<ResolvedNode> children,
      @Nullable ResolvedTag companionSibling)
      implements ResolvedNode {}

  /// Transparent group of sibling nodes that emits no wrapper.
  record ResolvedFragment(List<ResolvedNode> children) implements ResolvedNode {
    public ResolvedFragment {
      children = List.copyOf(children);
    }
  }

  record ResolvedText(String content) implements ResolvedNode {}

  record ResolvedRaw(String trustedHtml) implements ResolvedNode {}

  enum ResolvedEmpty implements ResolvedNode {
    INSTANCE
  }

  /// Position of a [LateElement]; replaced by its resolved content in the link pass.
  record ResolvedSlot(int index) implements ResolvedNode {}
}
