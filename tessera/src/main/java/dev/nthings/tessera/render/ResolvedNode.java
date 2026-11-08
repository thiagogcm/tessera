package dev.nthings.tessera.render;

import dev.nthings.tessera.core.IdReference;
import dev.nthings.tessera.core.TagName;
import java.util.List;
import java.util.Map;

/// Linked page content: what validation, inspection and painting walk.
sealed interface ResolvedNode
    permits ResolvedNode.ResolvedTag, ResolvedNode.ResolvedText, ResolvedNode.ResolvedRaw {

  /// Linked element; `idReferences` keeps the references whose targets were reported as missing
  /// or ambiguous, so validators skip secondary errors for them.
  record ResolvedTag(
      TagName tag,
      CompilationLocation location,
      Map<String, String> attributes,
      Map<String, IdReference> idReferences,
      List<ResolvedNode> children)
      implements ResolvedNode {}

  record ResolvedText(String content) implements ResolvedNode, ViewNode {}

  record ResolvedRaw(String trustedHtml) implements ResolvedNode, ViewNode {}
}
