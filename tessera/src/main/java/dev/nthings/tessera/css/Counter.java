package dev.nthings.tessera.css;

import org.jspecify.annotations.Nullable;

/// A CSS counter: reset it with [Styled#counterReset(Counter)], step it with
/// [Styled#counterIncrement(Counter)], and print it with [CssContent#counter(Counter)].
public final class Counter implements CssValue {

  private final CssIdent ident;

  private Counter(CssIdent ident) {
    this.ident = ident;
  }

  /// The counter `name`, such as `step`. Throws IllegalArgumentException if `name` is not a CSS
  /// identifier, starts with `--`, or is `none`, `default` or a CSS-wide keyword.
  public static Counter named(String name) {
    return new Counter(CssIdent.of(name));
  }

  @Override
  public String toCss() {
    return ident.toCss();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof Counter counter && counter.ident.equals(ident);
  }

  @Override
  public int hashCode() {
    return ident.hashCode();
  }

  @Override
  public String toString() {
    return toCss();
  }
}
