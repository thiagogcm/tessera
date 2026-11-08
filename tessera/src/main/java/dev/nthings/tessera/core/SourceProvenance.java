package dev.nthings.tessera.core;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Compiler-facing: binds the source capture for one expansion turn. Elements read their call site
/// from here when they are created.
public final class SourceProvenance {

  private static final ScopedValue<SourceCapture> CAPTURE = ScopedValue.newInstance();

  private SourceProvenance() {}

  /// Compiler-facing: runs `work` with `capture` bound as the current source capture.
  public static <T> T with(SourceCapture capture, Supplier<T> work) {
    return ScopedValue.where(CAPTURE, capture).call(work::get);
  }

  /// Compiler-facing: the capture bound to this turn, or [SourceCapture#NONE].
  public static SourceCapture current() {
    return CAPTURE.orElse(SourceCapture.NONE);
  }

  /// Compiler-facing: the call site for an element built now, or `null` when tracking is off.
  public static @Nullable SourceLocation capture() {
    return current().capture();
  }
}
