package dev.nthings.tessera.foundation;

import java.util.function.BiFunction;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Composable fluent conditionals without unchecked self-casts. Element and builder types implement
/// it, so a chain can branch without breaking its fluent flow.
///
/// @param <SELF> concrete element or builder type
public interface FluentBuilder<SELF extends FluentBuilder<SELF>> {

  /// Returns this builder, typed as `SELF`.
  SELF self();

  /// Applies `then` when `condition` holds, and returns this builder unchanged otherwise.
  default SELF onlyIf(boolean condition, Function<SELF, SELF> then) {
    return condition ? then.apply(self()) : self();
  }

  /// Applies `then` with `value` when `value` is non-null, and returns this builder unchanged
  /// otherwise.
  default <T> SELF ifPresent(@Nullable T value, BiFunction<SELF, T, SELF> then) {
    return value != null ? then.apply(self(), value) : self();
  }
}
