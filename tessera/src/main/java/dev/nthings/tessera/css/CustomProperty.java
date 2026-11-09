package dev.nthings.tessera.css;

/// A custom property local to some elements, such as a per-item index: set it with
/// [StyleRefinement#customProperty(CustomProperty, CssValue)] and read it with
/// [CssNumber#property(CustomProperty)] or [CssContent#property(CustomProperty)]. Theme-wide
/// values belong in a [DesignToken] instead. Serializes as `--name`.
public final class CustomProperty extends NamedValue {

  private CustomProperty(CssIdent ident) {
    super(ident);
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
}
