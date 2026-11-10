package dev.nthings.tessera.view;

/// The namespace that generated and scoped DOM ids live in. Each [dev.nthings.tessera.core.Scope]
/// opens one, nested where it is placed, so `domId` joins every enclosing scope id with `--`.
/// Keyed content opens one too; `keyed` marks that scope and every scope nested in it.
/// `generated` is set when any segment of `domId` came from an encounter-order counter rather than
/// an explicit [dev.nthings.tessera.core.Scope#scopeId]. The page itself is [#ROOT].
record ViewScope(String domId, boolean keyed, boolean generated) {

  static final ViewScope ROOT = new ViewScope("", false, false);

  /// The scope nested inside this one under the sanitized local id `slug`. `generatedSlug` marks
  /// `slug` as assigned by the compiler, which sets [#generated].
  ViewScope extend(String slug, boolean generatedSlug) {
    return new ViewScope(qualify(slug), keyed, generated || generatedSlug);
  }

  /// The scope of keyed content nested inside this one under the sanitized local id `slug`.
  ViewScope extendKeyed(String slug) {
    return new ViewScope(qualify(slug), true, generated);
  }

  /// The DOM id of the sanitized local id `slug` in this scope.
  String qualify(String slug) {
    return domId.isEmpty() ? slug : domId + "--" + slug;
  }
}
