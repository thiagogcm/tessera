package dev.nthings.tessera.compiler;

/// The namespace that generated and scoped DOM ids live in. Each `Scope` instance opens one,
/// nested in the scope where it is placed, so `domId` joins the id of every enclosing scope with
/// `--`. Keyed content opens one too; `keyed` marks such a scope and every scope nested in it.
/// `generated` is set when any segment of `domId` came from an encounter-order counter rather than
/// an explicit [dev.nthings.tessera.core.Scope#scopeId]. The page itself is [#ROOT].
record IdScope(String domId, boolean keyed, boolean generated) {

  static final IdScope ROOT = new IdScope("", false, false);

  /// The scope nested inside this one under the sanitized local id `slug`.
  IdScope extend(String slug) {
    return new IdScope(qualify(slug), keyed, generated);
  }

  /// The scope of keyed content nested inside this one under the sanitized local id `slug`.
  IdScope extendKeyed(String slug) {
    return new IdScope(qualify(slug), true, generated);
  }

  /// This scope with [#generated] set, because its own id was assigned by the compiler.
  IdScope asGenerated() {
    return generated ? this : new IdScope(domId, keyed, true);
  }

  /// The DOM id of the sanitized local id `slug` in this scope.
  String qualify(String slug) {
    return domId.isEmpty() ? slug : domId + "--" + slug;
  }
}
