package dev.nthings.tessera.compiler;

import java.util.List;
import java.util.stream.Collectors;

/// Authoring errors found while compiling a page or site. Compilation collects every independent
/// error before throwing, and no HTML is emitted. The message lists each [Diagnostic#format()].
public final class CompilationException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private final Diagnostic[] diagnostics;

  CompilationException(List<Diagnostic> diagnostics) {
    super(format(diagnostics));
    this.diagnostics = diagnostics.toArray(Diagnostic[]::new);
  }

  /// The diagnostics in the order they were reported. Site compilation reports route and file
  /// path conflicts on their own; otherwise page errors in route order; otherwise cross-route
  /// errors.
  public List<Diagnostic> diagnostics() {
    return List.of(diagnostics);
  }

  private static String format(List<Diagnostic> diagnostics) {
    return "Compilation failed with "
        + diagnostics.size()
        + (diagnostics.size() == 1 ? " error:\n" : " errors:\n")
        + diagnostics.stream().map(Diagnostic::format).collect(Collectors.joining("\n"));
  }
}
