package dev.nthings.tessera.css;

import java.util.List;
import java.util.stream.Collectors;

/// One `position-try-fallbacks` candidate: flips tried together on overflow.
public record PositionTry(List<Flip> flips) implements CssValue {

  /// Axes an anchored element can flip across its anchor.
  public enum Flip implements CssKeyword {
    BLOCK,
    INLINE,
    START;

    @Override
    public String toCss() {
      return "flip-" + CssKeyword.super.toCss();
    }
  }

  public PositionTry {
    if (flips.isEmpty()) {
      throw new IllegalArgumentException("A fallback needs at least one flip");
    }
    flips = List.copyOf(flips);
  }

  public static PositionTry flip(Flip... flips) {
    return new PositionTry(List.of(flips));
  }

  @Override
  public String toCss() {
    return flips.stream().map(Flip::toCss).collect(Collectors.joining(" "));
  }
}
