package dev.nthings.tessera.core;

/// DOM id prefix formed by joining the [ElementId] of every enclosing [RenderView] with `--`.
public record ViewScope(String domId) {

  public static final ViewScope ROOT = new ViewScope("");

  public ViewScope extend(ElementId childId) {
    return new ViewScope(domId.isEmpty() ? childId.slug() : domId + "--" + childId.slug());
  }
}
