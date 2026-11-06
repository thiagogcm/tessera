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
        + diagnostics.stream()
            .map(CompilationException::formatDiagnostic)
            .collect(Collectors.joining("\n"));
  }

  private static String formatDiagnostic(CompilationDiagnostic diagnostic) {
    var location = diagnostic.location();
    var route = location.route();
    var components = location.componentPath();
    return "["
        + diagnostic.rule()
        + "] "
        + (route != null ? "Route \"" + route + "\", " : "")
        + "page \""
        + location.pageTitle()
        + "\""
        + (components.isEmpty() ? "" : " > " + String.join(" > ", components))
        + " > "
        + "<"
        + location.tagName()
        + (location.domId() == null ? "" : " id=\"" + location.domId() + "\"")
        + ">"
        + (location.source() == null ? "" : " (" + location.source() + ")")
        + ": "
        + diagnostic.message()
        + "\n  Fix: "
        + diagnostic.suggestion();
  }
}
