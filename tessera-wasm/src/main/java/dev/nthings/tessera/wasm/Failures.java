package dev.nthings.tessera.wasm;

import java.util.List;
import org.graalvm.webimage.api.ThrownFromJavaScript;

/// Readable browser reports for Java failures, which otherwise surface as opaque proxies.
final class Failures {
  private Failures() {}

  /// `what` qualified with the browser root it happened in.
  static String inRoot(String what, String hostId) {
    return what + " in browser root '" + hostId + "'";
  }

  /// Reports an application failure in `root` as a readable browser error.
  static void report(String what, String hostId, RuntimeException failure) {
    Dom.reportError(inRoot(what + " failed", hostId) + ": " + describe(failure));
  }

  /// Runs application `work`, reporting its failure without rethrowing.
  static void run(String what, String hostId, Runnable work) {
    try {
      work.run();
    } catch (RuntimeException failure) {
      report(what, hostId, failure);
    }
  }

  /// Runs `step`, collecting its failure so later steps still run.
  static void attempt(List<RuntimeException> failures, Runnable step) {
    try {
      step.run();
    } catch (RuntimeException failure) {
      failures.add(failure);
    }
  }

  /// Reports collected failures as one browser error under `header`, then clears them.
  static void reportAll(String header, List<RuntimeException> failures) {
    if (failures.isEmpty()) {
      return;
    }
    var message = new StringBuilder(header).append(':');
    failures.forEach(failure -> message.append('\n').append(describe(failure)));
    failures.clear();
    Dom.reportError(message.toString());
  }

  /// The failure's type and message, followed by the portable diagnostics attached to it, such as
  /// the component path of a failed render callback.
  static String describe(Throwable failure) {
    var text = new StringBuilder();
    if (!(failure instanceof ThrownFromJavaScript)) {
      text.append(failure.getClass().getName()).append(": ");
    }
    text.append(failure.getMessage());
    for (var context : failure.getSuppressed()) {
      text.append('\n').append(context.getMessage());
    }
    return text.toString();
  }
}
