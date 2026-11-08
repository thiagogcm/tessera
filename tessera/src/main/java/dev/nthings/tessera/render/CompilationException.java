package dev.nthings.tessera.render;

import java.util.List;
import java.util.stream.Collectors;

/// Authoring errors found during page or site compilation.
/// HTML is emitted only after validation succeeds.
public final class CompilationException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private final CompilationDiagnostic[] diagnostics;

  /// Compiler-facing: wraps the diagnostics collected for one compilation.
  public CompilationException(List<CompilationDiagnostic> diagnostics) {
    super(format(diagnostics));
    this.diagnostics = diagnostics.toArray(CompilationDiagnostic[]::new);
  }

  /// The diagnostics in the order they were reported. Site compilation lists page errors in route
  /// order before cross-route errors.
  public List<CompilationDiagnostic> diagnostics() {
    return List.of(diagnostics);
  }

  private static String format(List<CompilationDiagnostic> diagnostics) {
    return "Compilation failed with "
        + diagnostics.size()
        + (diagnostics.size() == 1 ? " error:\n" : " errors:\n")
        + diagnostics.stream().map(CompilationDiagnostic::format).collect(Collectors.joining("\n"));
  }
}
