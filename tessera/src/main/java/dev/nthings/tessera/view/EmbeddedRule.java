package dev.nthings.tessera.view;

import org.jspecify.annotations.Nullable;

/// Backend-facing: one self-contained rule of the embedded output. Its `key` is short and
/// identifies the rule's text, so an embedder can keep the text it built for a key, compare renders
/// by key and build [#css()] only for rules it does not hold.
public final class EmbeddedRule {
  private final String key;
  private final @Nullable String layer;
  // The rule is prefix + body + suffix, joined only when its text is needed.
  private final String prefix;
  private final String body;
  private final String suffix;

  public EmbeddedRule(String key, @Nullable String layer, String rule) {
    this(key, layer, "", rule, "");
  }

  public EmbeddedRule(
      String key, @Nullable String layer, String prefix, String body, String suffix) {
    this.key = key;
    this.layer = layer;
    this.prefix = prefix;
    this.body = body;
    this.suffix = suffix;
  }

  /// The short key identifying this rule's text: a class name for element rules, and a selector,
  /// property name or the rule itself for the others.
  public String key() {
    return key;
  }

  /// The rule wrapped in its cascade layer; built on each call.
  public String css() {
    var rule = prefix + body + suffix;
    return layer == null ? rule : "@layer " + layer + "{" + rule + "}";
  }

  public void appendRule(StringBuilder into) {
    into.append(prefix).append(body).append(suffix);
  }

  public @Nullable String layer() {
    return layer;
  }
}
