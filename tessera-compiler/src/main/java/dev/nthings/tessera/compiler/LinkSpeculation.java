package dev.nthings.tessera.compiler;

import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.Nullable;

/// Same-site speculation through declarative Speculation Rules. Links are prefetched or prerendered
/// on hover or pointer-down (`moderate` eagerness).
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

  @Nullable String inlineRules() {
    return rulesJson;
  }

  @Nullable Asset externalRules(String assetBase) {
    var json = rulesJson;
    if (json == null) {
      return null;
    }
    return new Asset(
        HtmlWriter.hashedPath(assetBase, "speculation", json, ".json"),
        "application/speculationrules+json",
        json.getBytes(StandardCharsets.UTF_8));
  }
}
