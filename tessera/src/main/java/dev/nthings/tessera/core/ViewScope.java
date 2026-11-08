package dev.nthings.tessera.core;

/// A view scope is the namespace that generated and explicit DOM ids live in. Each [RenderView]
/// instance opens one, nested in the scope where it is placed, so `domId` joins the [ElementId] of
/// every enclosing view with `--`. Inside a view with id `toolbar`, `ElementRef.named("save")` gets
/// the DOM id `toolbar--save`. The page itself is [#ROOT].
public record ViewScope(String domId) {

  /// The page-level scope, with an empty DOM id prefix.
  public static final ViewScope ROOT = new ViewScope("");

  /// Returns the scope nested inside this one under `childId`.
  public ViewScope extend(ElementId childId) {
    return new ViewScope(domId.isEmpty() ? childId.slug() : domId + "--" + childId.slug());
  }
}
