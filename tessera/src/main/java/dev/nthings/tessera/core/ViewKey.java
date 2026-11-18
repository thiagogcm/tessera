package dev.nthings.tessera.core;

import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import java.util.Map;
import java.util.function.Supplier;

/// Typed state for one expansion, created on first use by [ViewContext#state(ViewKey)]. An
/// expansion is one compiled page or one attempted browser render, so the state is discarded with a
/// rejected render and never carries over to the next one. Keep application state that must survive
/// renders in the application. Keys compare by identity, so declare them `static final`: a key
/// created inside an expand callback is a new slot on each call. `initial` runs once per expansion.
///
/// ```java
/// private static final ViewKey<AtomicInteger> NOTES =
///     ViewKey.of("notes", AtomicInteger.class, AtomicInteger::new);
///
/// int number = context.state(NOTES).incrementAndGet();
/// ```
public final class ViewKey<T> {

  static {
    AuthoringAccess.registerState(
        new AuthoringAccess.StateAccess() {
          @Override
          public <V> V getOrCreate(ViewKey<V> key, Map<ViewKey<?>, Object> values) {
            return key.type.cast(values.computeIfAbsent(key, _ -> key.initial.get()));
          }
        });
  }

  private final String name;
  private final Class<T> type;
  private final Supplier<? extends T> initial;

  private ViewKey(String name, Class<T> type, Supplier<? extends T> initial) {
    this.name = name;
    this.type = type;
    this.initial = initial;
  }

  /// Creates a key whose state `initial` supplies on first use. `name` labels the key in
  /// `toString()` and does not affect its identity.
  public static <T> ViewKey<T> of(String name, Class<T> type, Supplier<? extends T> initial) {
    return new ViewKey<>(name, type, initial);
  }

  /// The label given to [#of].
  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return "ViewKey(" + name + ")";
  }
}
