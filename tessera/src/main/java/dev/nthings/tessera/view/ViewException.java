package dev.nthings.tessera.view;

import java.util.List;
import java.util.stream.Collectors;

/// Authoring errors found while rendering a mounted view with [ViewRenderer]. Rendering collects
/// every independent error before throwing; the message lists each [Diagnostic#format()].
public final class ViewException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private final Diagnostic[] diagnostics;

  /// Backend-facing: an exception carrying `diagnostics`, in reporting order.
  public ViewException(List<Diagnostic> diagnostics) {
    super(diagnostics.stream().map(Diagnostic::format).collect(Collectors.joining("\n")));
    this.diagnostics = diagnostics.toArray(Diagnostic[]::new);
  }

  /// The diagnostics in the order they were reported.
  public List<Diagnostic> diagnostics() {
    return List.of(diagnostics);
  }
}
