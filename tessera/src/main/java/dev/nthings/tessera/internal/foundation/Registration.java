package dev.nthings.tessera.internal.foundation;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// A value installed once, during the class initialization of the type that owns its data, and read
/// by other packages of the library. When nothing is installed yet, [#get()] calls `initialize`
/// first, which uses the owning class and so triggers its initialization.
public final class Registration<T> {
  private final String name;
  private final @Nullable Supplier<?> initialize;
  private final AtomicReference<@Nullable T> value = new AtomicReference<>();

  /// A registration whose owner is always initialized before its value is read.
  public Registration(String name) {
    this.name = name;
    this.initialize = null;
  }

  /// A registration whose owner may still be uninitialized when its value is first read.
  public Registration(String name, Supplier<?> initialize) {
    this.name = name;
    this.initialize = initialize;
  }

  /// Installs `value`. Throws IllegalStateException if a value is already installed.
  public void register(T value) {
    if (!this.value.compareAndSet(null, value)) {
      throw new IllegalStateException(name + " already registered");
    }
  }

  /// The installed value. Throws IllegalStateException if initializing the owner installed none.
  public T get() {
    var current = value.get();
    if (current == null && initialize != null) {
      Objects.requireNonNull(initialize.get(), name);
      current = value.get();
    }
    if (current == null) {
      throw new IllegalStateException("No " + name + " registered");
    }
    return current;
  }
}
