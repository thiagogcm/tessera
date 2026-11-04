package dev.nthings.tessera.site;

import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.TagElement;
import org.jspecify.annotations.Nullable;

/// Same-site navigation speculation emitted as declarative `<script type="speculationrules">`
/// (no JavaScript executes): links are prefetched or prerendered on hover/pointer-down
/// (`moderate` eagerness), making route changes and their cross-document view transitions
/// near-instant.
public enum LinkSpeculation {
  NONE,
  PREFETCH,
  PRERENDER;

  /// The rules element for `<head>`, or `null` for [#NONE].
  @Nullable TagElement rulesElement() {
    if (this == NONE) {
      return null;
    }
    String action = this == PRERENDER ? "prerender" : "prefetch";
    String json =
        "{\"" + action + "\":[{\"where\":{\"href_matches\":\"/*\"},\"eagerness\":\"moderate\"}]}";
    return new TagElement(HtmlTag.SCRIPT)
        .attr("type", "speculationrules")
        .child(Elements.rawHtml(json));
  }
}
