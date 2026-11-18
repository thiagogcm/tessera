package dev.nthings.tessera.compiler;

import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.Documents;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Same-site speculation as declarative `<script type="speculationrules">` (no JavaScript): links
/// are prefetched or prerendered on hover or pointer-down (`moderate` eagerness).
public enum LinkSpeculation {
  /// Emits no speculation rules.
  NONE(null),
  /// Prefetches same-site links with `moderate` eagerness.
  PREFETCH("prefetch"),
  /// Prerenders same-site links with `moderate` eagerness.
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
  List<DocumentContent> headElements() {
    if (rulesJson == null) {
      return List.of();
    }
    return List.of(Documents.script("speculationrules", rulesJson));
  }
}
