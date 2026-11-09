package dev.nthings.tessera.core;

import java.util.Map;
import java.util.function.Supplier;

/// Typed state for one page, created on first use by [RenderContext#document(DocumentKey)]. Keys
/// compare by identity, so declare them `static final`; a key created inside a render callback is a
/// new slot on each call, so its state is never shared. `initial` runs once per page compilation.
///
/// ```java
/// private static final DocumentKey<AtomicInteger> NOTES =
///     DocumentKey.of("notes", AtomicInteger.class, AtomicInteger::new);
///
/// int number = context.document(NOTES).incrementAndGet();
/// ```
public final class DocumentKey<T> {

  private final String name;
  private final Class<T> type;
  private final Supplier<? extends T> initial;

  private DocumentKey(String name, Class<T> type, Supplier<? extends T> initial) {
    this.name = name;
    this.type = type;
    this.initial = initial;
  }

  /// Creates a key whose state `initial` supplies on first use. `name` labels the key in
  /// `toString()` and does not affect its identity.
  public static <T> DocumentKey<T> of(String name, Class<T> type, Supplier<? extends T> initial) {
    return new DocumentKey<>(name, type, initial);
  }

  /// The label given to [#of].
  public String name() {
    return name;
  }

  /// Compiler-facing: this key's state in `values`, created with the initial supplier on first
  /// use.
  public T getOrCreate(Map<DocumentKey<?>, Object> values) {
    return type.cast(values.computeIfAbsent(this, _ -> initial.get()));
  }

  @Override
  public String toString() {
    return "DocumentKey(" + name + ")";
  }
}
