package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.TagName;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Linked page content: what validation, inspection and painting walk.
sealed interface ResolvedNode
    permits ResolvedNode.ResolvedTag, ResolvedNode.ResolvedText, ResolvedNode.ResolvedRaw {

  /// Linked element. `unresolvedAttributes` names reference attributes already reported as
  /// missing or ambiguous, so validators skip them.
  record ResolvedTag(
      TagName tag,
      NodeLocation location,
      Map<String, String> attributes,
      Set<String> unresolvedAttributes,
      List<ResolvedNode> children)
      implements ResolvedNode {}

  record ResolvedText(String content) implements ResolvedNode, ExpandedNode {}

  record ResolvedRaw(String trustedHtml) implements ResolvedNode, ExpandedNode {}
}
