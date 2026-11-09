package dev.nthings.tessera.css;

import dev.nthings.tessera.foundation.Decimals;
import java.util.List;
import java.util.stream.Collectors;

/// The [Transform] implementations, built only through its factories and [Transform#then].
final class Transforms {

  private Transforms() {}

  /// `scale(factor)`.
  record Scale(double factor) implements Transform {
    @Override
    public String toCss() {
      return "scale(" + Decimals.format(factor) + ")";
    }
  }

  /// `scaleX(factor)`.
  record ScaleX(double factor) implements Transform {
    @Override
    public String toCss() {
      return "scaleX(" + Decimals.format(factor) + ")";
    }
  }

  /// `rotate(<degrees>deg)`.
  record Rotate(double degrees) implements Transform {
    @Override
    public String toCss() {
      return "rotate(" + Decimals.format(degrees) + "deg)";
    }
  }

  /// `translateX(distance)`.
  record TranslateX(CssLength distance) implements Transform {
    @Override
    public String toCss() {
      return "translateX(" + distance.toCss() + ")";
    }
  }

  /// `translateY(distance)`.
  record TranslateY(CssLength distance) implements Transform {
    @Override
    public String toCss() {
      return "translateY(" + distance.toCss() + ")";
    }
  }

  /// Functions written space-separated in the CSS `transform` list, in the order given.
  record Chain(List<Transform> steps) implements Transform {
    Chain {
      steps = List.copyOf(steps);
    }

    @Override
    public String toCss() {
      return steps.stream().map(Transform::toCss).collect(Collectors.joining(" "));
    }
  }
}
