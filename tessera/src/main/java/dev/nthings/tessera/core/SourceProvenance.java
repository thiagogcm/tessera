package dev.nthings.tessera.core;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Turn-scoped source capture that avoids a platform stack walker.
public final class SourceProvenance {

  private static final ScopedValue<SourceCapture> CAPTURE = ScopedValue.newInstance();

  private SourceProvenance() {}

  public static <T> T with(SourceCapture capture, Supplier<T> work) {
    return ScopedValue.where(CAPTURE, capture).call(work::get);
  }

  /// The capture bound to this turn, or [SourceCapture#NONE].
  public static SourceCapture current() {
    return CAPTURE.orElse(SourceCapture.NONE);
  }

  public static @Nullable SourceLocation capture() {
    return current().capture();
  }
}
