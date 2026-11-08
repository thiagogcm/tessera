package dev.nthings.tessera.site;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.TagElement;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Same-site speculation as declarative `<script type="speculationrules">` (no JavaScript):
/// links are prefetched or prerendered on hover or pointer-down (`moderate` eagerness).
public enum LinkSpeculation {
  NONE(null),
  PREFETCH("prefetch"),
  PRERENDER("prerender");

  private final @Nullable String rulesJson;

  LinkSpeculation(@Nullable String action) {
    this.rulesJson =
        action == null
            ? null
            : "{\""
                + action
                + "\":[{\"where\":{\"href_matches\":\"/*\"},\"eagerness\":\"moderate\"}]}";
  }

  /// Site-wide `<head>` content: the rules element, or nothing for [#NONE].
  List<Element> headElements() {
    if (rulesJson == null) {
      return List.of();
    }
    return List.of(
        new TagElement(HtmlTag.SCRIPT)
            .attr("type", "speculationrules")
            .child(Elements.rawHtml(rulesJson)));
  }
}
