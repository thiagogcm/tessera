package dev.nthings.tessera.support;

import dev.nthings.tessera.compiler.CompilationException;
import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.view.ViewException;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.jspecify.annotations.Nullable;

/// Reported problems reduced to what an author acts on: which rule, where, and on which element.
/// Tests assert these facts instead of message or fix prose.
public final class Diagnostics {

  private Diagnostics() {}

  /// One reported problem.
  public record Found(
      String rule,
      @Nullable String route,
      String tag,
      @Nullable String domId,
      List<String> components) {

    static Found of(Diagnostic diagnostic) {
      var location = diagnostic.location();
      return new Found(
          diagnostic.rule(),
          location.route(),
          location.tag(),
          location.domId(),
          location.components());
    }
  }

  /// The problems `action` reports; fails if it completes or throws anything else.
  public static List<Found> of(ThrowingCallable action) {
    try {
      action.call();
    } catch (CompilationException error) {
      return error.diagnostics().stream().map(Found::of).toList();
    } catch (ViewException error) {
      return error.diagnostics().stream().map(Found::of).toList();
    } catch (Throwable error) {
      throw new AssertionError("Expected reported diagnostics but got " + error, error);
    }
    throw new AssertionError("Expected reported diagnostics but the action succeeded");
  }

  /// The rules `action` reports, in report order.
  public static List<String> rules(ThrowingCallable action) {
    return of(action).stream().map(Found::rule).toList();
  }
}
