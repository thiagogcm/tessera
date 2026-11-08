package dev.nthings.tessera.core;

import java.util.function.Supplier;

/// Typed per-page state slot in a [DocumentState], created on first use. Compared by identity.
///
/// @param <T> state type
public final class DocumentKey<T> {

  private final String name;
  private final Class<T> type;
  private final Supplier<? extends T> initial;

  private DocumentKey(String name, Class<T> type, Supplier<? extends T> initial) {
    this.name = name;
    this.type = type;
    this.initial = initial;
  }

  public static <T> DocumentKey<T> of(String name, Class<T> type, Supplier<? extends T> initial) {
    return new DocumentKey<>(name, type, initial);
  }

  public String name() {
    return name;
  }

  T cast(Object value) {
    return type.cast(value);
  }

  T create() {
    return initial.get();
  }

  @Override
  public String toString() {
    return "DocumentKey(" + name + ")";
  }
}
