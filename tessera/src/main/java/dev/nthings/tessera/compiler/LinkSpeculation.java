package dev.nthings.tessera.compiler;

import dev.nthings.tessera.foundation.Fnv1a64;
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

  @Nullable Asset externalRules(ExternalOutput output) {
    var json = rulesJson;
    if (json == null) {
      return null;
    }
    var hash = Fnv1a64.toBase36Hash(Fnv1a64.updateChars(Fnv1a64.OFFSET_BASIS, json));
    return new Asset(
        output.base() + "/speculation-" + hash + ".json",
        "application/speculationrules+json",
        json.getBytes(StandardCharsets.UTF_8));
  }
}
