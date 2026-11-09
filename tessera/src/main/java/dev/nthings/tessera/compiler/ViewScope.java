package dev.nthings.tessera.compiler;

/// The namespace that generated and scoped DOM ids live in. Each `View` instance opens one,
/// nested in the scope where it is placed, so `domId` joins the id of every enclosing view with
/// `--`. Keyed content opens one too; `keyed` marks such a scope and every scope nested in it. The
/// page itself is [#ROOT].
record ViewScope(String domId, boolean keyed) {

  static final ViewScope ROOT = new ViewScope("", false);

  /// The scope nested inside this one under the sanitized local id `slug`.
  ViewScope extend(String slug) {
    return new ViewScope(qualify(slug), keyed);
  }

  /// The scope of keyed content nested inside this one under the sanitized local id `slug`.
  ViewScope extendKeyed(String slug) {
    return new ViewScope(qualify(slug), true);
  }

  /// The DOM id of the sanitized local id `slug` in this scope.
  String qualify(String slug) {
    return domId.isEmpty() ? slug : domId + "--" + slug;
  }
}
