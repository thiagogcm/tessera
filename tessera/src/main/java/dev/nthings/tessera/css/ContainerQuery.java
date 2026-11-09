package dev.nthings.tessera.css;

import org.jspecify.annotations.Nullable;

/// A condition on an ancestor container, emitted as an `@container` rule. Get one from a
/// [Container], such as `container.atLeast(CssLength.rem(36))`; queries on an unnamed container
/// match the nearest ancestor container of the right type.
public final class ContainerQuery implements Condition.Query {

  private final String prelude;

  ContainerQuery(Container container, String condition) {
    var name = container.ident();
    this.prelude = "@container " + (name == null ? "" : name.toCss() + " ") + condition;
  }

  /// The prelude that opens the nested rule, such as `@container card (inline-size >= 28rem)`.
  String prelude() {
    return prelude;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof ContainerQuery query && query.prelude.equals(prelude);
  }

  @Override
  public int hashCode() {
    return prelude.hashCode();
  }

  @Override
  public String toString() {
    return prelude;
  }
}
