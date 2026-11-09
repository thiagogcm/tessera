package dev.nthings.tessera.compiler;

/// The namespace that generated and scoped DOM ids live in. Each `View` instance opens one,
/// nested in the scope where it is placed, so `domId` joins the id of every enclosing view with
/// `--`. The page itself is [#ROOT].
record ViewScope(String domId) {

  static final ViewScope ROOT = new ViewScope("");

  /// The scope nested inside this one under the sanitized local id `slug`.
  ViewScope extend(String slug) {
    return new ViewScope(qualify(slug));
  }

  /// The DOM id of the sanitized local id `slug` in this scope.
  String qualify(String slug) {
    return domId.isEmpty() ? slug : domId + "--" + slug;
  }
}
