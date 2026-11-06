package dev.nthings.tessera.css;

/// A `scroll-snap-type`: the snapping axis and how strictly the scroller snaps.
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

  public static ScrollSnap mandatory(Axis axis) {
    return new ScrollSnap(axis, Strictness.MANDATORY);
  }

  @Override
  public String toCss() {
    return axis.toCss() + " " + strictness.toCss();
  }
}
