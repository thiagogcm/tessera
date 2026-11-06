package dev.nthings.tessera.render;

import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IdReference;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

sealed interface ResolvedNode
    permits ResolvedNode.ResolvedTag,
        ResolvedNode.ResolvedText,
        ResolvedNode.ResolvedRaw,
        ResolvedNode.ResolvedEmpty,
        ResolvedNode.ResolvedSlot {

  /// Resolved element; `idReferences` are pending until the prepaint link pass folds them into
  /// `attributes`.
  record ResolvedTag(
      HtmlTag tag,
      CompilationLocation location,
      Map<String, String> attributes,
      Map<String, IdReference> idReferences,
      List<ResolvedNode> children,
      @Nullable ResolvedTag companionSibling)
      implements ResolvedNode {}

  record ResolvedText(String content) implements ResolvedNode {}

  record ResolvedRaw(String trustedHtml) implements ResolvedNode {}

  enum ResolvedEmpty implements ResolvedNode {
    INSTANCE
  }

  /// Position of a [LateElement]; replaced by its resolved content in the link pass.
  record ResolvedSlot(int index) implements ResolvedNode {}
}
