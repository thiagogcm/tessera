package dev.nthings.tessera.css;

/// A `scroll-snap-type`: snapping axis and strictness. Applied with
/// [Styled#scrollSnapType].
public record ScrollSnap(Axis axis, Strictness strictness) implements CssValue {

  public enum Axis implements CssKeyword {
    X,
    Y,
    BLOCK,
    INLINE,
    BOTH
  }

  public enum Strictness implements CssKeyword {
    PROXIMITY,
    MANDATORY
  }

  /// `<axis> mandatory`: snaps to child positions along `axis`, always.
  public static ScrollSnap mandatory(Axis axis) {
    return new ScrollSnap(axis, Strictness.MANDATORY);
  }

  @Override
  public String toCss() {
    return axis.toCss() + " " + strictness.toCss();
  }
}
