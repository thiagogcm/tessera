package dev.nthings.tessera.render;

import java.util.List;
import java.util.stream.Collectors;

/// Reports independent authoring errors found during page or site compilation.
/// HTML is emitted only after validation succeeds.
public final class CompilationException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private final CompilationDiagnostic[] diagnostics;

  public CompilationException(List<CompilationDiagnostic> diagnostics) {
    super(format(diagnostics));
    this.diagnostics = diagnostics.toArray(CompilationDiagnostic[]::new);
  }

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
