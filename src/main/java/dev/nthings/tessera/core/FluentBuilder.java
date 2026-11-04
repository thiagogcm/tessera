package dev.nthings.tessera.core;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Composable fluent conditional and mapping operations without unchecked self-casts.
///
/// @param <SELF> concrete element or builder type
public interface FluentBuilder<SELF extends FluentBuilder<SELF>> {

  SELF self();

  default SELF when(boolean condition, Function<SELF, SELF> then) {
    return condition ? then.apply(self()) : self();
  }

  default SELF whenElse(
      boolean condition, Function<SELF, SELF> then, Function<SELF, SELF> otherwise) {
    return condition ? then.apply(self()) : otherwise.apply(self());
  }

  default <T> SELF whenPresent(@Nullable T value, BiFunction<SELF, T, SELF> then) {
    return value != null ? then.apply(self(), value) : self();
  }

  default <T> SELF whenOptional(Optional<T> optional, BiFunction<SELF, T, SELF> then) {
    if (optional.isPresent()) {
      return then.apply(self(), optional.get());
    }
    return self();
  }

  default SELF map(Function<SELF, SELF> mapper) {
    return mapper.apply(self());
  }
}
