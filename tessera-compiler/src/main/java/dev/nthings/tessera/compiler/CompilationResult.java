package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.Diagnostic;
import java.util.List;

/// The outcome of compiling a page or site without throwing on authoring errors: either [Compiled]
/// with the output value, or [Failed] with the collected [Diagnostic]s.
///
/// @param <T> compiled output type ([CompiledPage] or [CompiledSite])
public sealed interface CompilationResult<T>
    permits CompilationResult.Compiled, CompilationResult.Failed {

  /// Compilation succeeded with `value`.
  record Compiled<T>(T value) implements CompilationResult<T> {}

  /// Compilation failed with one or more authoring `diagnostics`.
  record Failed<T>(List<Diagnostic> diagnostics) implements CompilationResult<T> {
    public Failed {
      if (diagnostics.isEmpty()) {
        throw new IllegalArgumentException(
            "Failed compilation must contain at least one diagnostic");
      }
      diagnostics = List.copyOf(diagnostics);
    }
  }

  /// Returns the compiled value, or throws [CompilationException] with the collected diagnostics.
  default T orThrow() {
    return switch (this) {
      case Compiled<T>(var value) -> value;
      case Failed<T>(var diagnostics) -> throw new CompilationException(diagnostics);
    };
  }
}
