package dev.nthings.tessera.css;

/// A CSS counter: reset it with [Styled#counterReset(Counter)], step it with
/// [Styled#counterIncrement(Counter)], and print it with [CssContent#counter(Counter)].
public final class Counter extends NamedValue {

  private Counter(CssIdent ident) {
    super(ident);
  }

  /// The counter `name`, such as `step`. Throws IllegalArgumentException if `name` is not a CSS
  /// identifier, starts with `--`, or is `none`, `default` or a CSS-wide keyword.
  public static Counter named(String name) {
    return new Counter(CssIdent.of(name));
  }
}
