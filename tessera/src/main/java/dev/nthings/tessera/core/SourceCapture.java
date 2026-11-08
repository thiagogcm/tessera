package dev.nthings.tessera.core;

import org.jspecify.annotations.Nullable;

/// Compiler-facing: captures the authoring call site of an element. Supplied by
/// `render.SourceTracking`.
@FunctionalInterface
public interface SourceCapture {

  /// Captures nothing. Used while source tracking is off.
  SourceCapture NONE = () -> null;

  /// Compiler-facing: the call site of the element being built, or `null` if none is known.
  @Nullable SourceLocation capture();
}
