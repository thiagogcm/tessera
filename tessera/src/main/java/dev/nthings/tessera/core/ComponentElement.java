package dev.nthings.tessera.core;

import org.jspecify.annotations.Nullable;

/// Compiler-facing: holds a [Component] until the compiler expands it. Created by
/// [Component#intoElement()]. `type` is the authoring class, named in diagnostic locations.
public record ComponentElement(Component component, Class<?> type, @Nullable SourceLocation source)
    implements Element {

  /// The component's name in diagnostic locations: the simple class name, or `Component` for a
  /// lambda or anonymous class.
  public String name() {
    return ClassNames.componentName(type);
  }
}
