package dev.nthings.tessera.css;

import org.jspecify.annotations.Nullable;

/// A custom property local to some elements, such as a per-item index: set it with
/// [StyleRefinement#customProperty(CustomProperty, CssValue)] and read it with
/// [CssNumber#property(CustomProperty)] or [CssContent#property(CustomProperty)]. Theme-wide
/// values belong in a [DesignToken] instead. Serializes as `--name`.
public final class CustomProperty implements CssValue {

  private final CssIdent ident;

  private CustomProperty(CssIdent ident) {
    this.ident = ident;
  }

  /// The custom property `--name`. Pass `name` without the leading `--`. Throws
  /// IllegalArgumentException if it is empty, starts with `-`, or has characters outside
  /// `[A-Za-z0-9_-]`.
  public static CustomProperty named(String name) {
    return new CustomProperty(CssIdent.custom(name));
  }

  /// `var(--name)`.
  String varReference() {
    return ident.varReference();
  }

  @Override
  public String toCss() {
    return ident.toCss();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof CustomProperty property && property.ident.equals(ident);
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
