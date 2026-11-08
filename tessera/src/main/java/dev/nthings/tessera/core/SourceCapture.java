package dev.nthings.tessera.core;

import org.jspecify.annotations.Nullable;

/// Captures the authoring call site of an element; supplied by `render.SourceTracking`.
@FunctionalInterface
public interface SourceCapture {

  SourceCapture NONE = () -> null;

  @Nullable SourceLocation capture();
}
