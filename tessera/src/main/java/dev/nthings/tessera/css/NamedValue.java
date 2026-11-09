package dev.nthings.tessera.css;

import org.jspecify.annotations.Nullable;

/// Identity shared by the public name handles ([Anchor], [Counter], [CustomProperty]): each wraps
/// one validated [CssIdent], serializes as it, and equals another handle of the same type with the
/// same name.
abstract sealed class NamedValue implements CssValue permits Anchor, Counter, CustomProperty {

  final CssIdent ident;

  NamedValue(CssIdent ident) {
    this.ident = ident;
  }

  @Override
  public final String toCss() {
    return ident.toCss();
  }

  @Override
  public final boolean equals(@Nullable Object other) {
    return other instanceof NamedValue named
        && named.getClass() == getClass()
        && named.ident.equals(ident);
  }

  @Override
  public final int hashCode() {
    return ident.hashCode();
  }

  @Override
  public final String toString() {
    return toCss();
  }
}
