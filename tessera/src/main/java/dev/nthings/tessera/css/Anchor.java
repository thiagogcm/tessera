package dev.nthings.tessera.css;

import org.jspecify.annotations.Nullable;

/// A CSS anchor name for anchor positioning: [Styled#anchorName(Anchor)] names an element, and
/// [Styled#positionAnchor(Anchor)] places another element against it. Serializes as `--name`.
public final class Anchor implements CssValue {

  private final CssIdent ident;

  private Anchor(CssIdent ident) {
    this.ident = ident;
  }

  /// The anchor `--name`. Pass `name` without the leading `--`. Throws IllegalArgumentException
  /// if it is empty, starts with `-`, or has characters outside `[A-Za-z0-9_-]`.
  public static Anchor named(String name) {
    return new Anchor(CssIdent.custom(name));
  }

  @Override
  public String toCss() {
    return ident.toCss();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof Anchor anchor && anchor.ident.equals(ident);
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
