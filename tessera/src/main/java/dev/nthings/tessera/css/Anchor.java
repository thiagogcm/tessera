package dev.nthings.tessera.css;

/// A CSS anchor name for anchor positioning: [Styled#anchorName(Anchor)] names an element, and
/// [Styled#positionAnchor(Anchor)] places another element against it. Serializes as `--name`.
public final class Anchor extends NamedValue {

  private Anchor(CssIdent ident) {
    super(ident);
  }

  /// The anchor `--name`. Pass `name` without the leading `--`. Throws IllegalArgumentException if
  /// it is empty, starts with `-`, or has characters outside `[A-Za-z0-9_-]`.
  public static Anchor named(String name) {
    return new Anchor(CssIdent.custom(name));
  }
}
