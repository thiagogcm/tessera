package dev.nthings.tessera.css;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/// A `transform` function or a sequence of them, applied with [EffectStyled#transform(Transform)].
public sealed interface Transform extends CssValue
    permits Transform.Scale, Transform.Rotate, Transform.TranslateY, Transform.Chain {

  /// `scale(factor)`.
  record Scale(double factor) implements Transform {
    @Override
    public String toCss() {
      return "scale(" + CssLength.formatNumber(factor) + ")";
    }
  }

  /// `rotate(<degrees>deg)`.
  record Rotate(double degrees) implements Transform {
    @Override
    public String toCss() {
      return "rotate(" + CssLength.formatNumber(degrees) + "deg)";
    }
  }

  /// `translateY(distance)`.
  record TranslateY(CssLength distance) implements Transform {
    @Override
    public String toCss() {
      return "translateY(" + distance.toCss() + ")";
    }
  }

  /// Functions written space-separated in the CSS `transform` list, in the order given. CSS
  /// composes them right to left, so the last function acts first on the element.
  record Chain(List<Transform> steps) implements Transform {
    public Chain {
      steps = List.copyOf(steps);
    }

    @Override
    public String toCss() {
      return steps.stream().map(Transform::toCss).collect(Collectors.joining(" "));
    }
  }

  static Transform scale(double factor) {
    return new Scale(factor);
  }

  static Transform rotate(double degrees) {
    return new Rotate(degrees);
  }

  static Transform translateY(CssLength distance) {
    return new TranslateY(distance);
  }

  /// A chain of this transform followed by `next`, written in that order in the CSS list.
  default Transform then(Transform next) {
    var steps = new ArrayList<Transform>();
    steps.addAll(this instanceof Chain chain ? chain.steps() : List.of(this));
    steps.addAll(next instanceof Chain chain ? chain.steps() : List.of(next));
    return new Chain(steps);
  }
}
