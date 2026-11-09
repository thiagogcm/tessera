package dev.nthings.tessera.css;

import java.util.ArrayList;
import java.util.List;

/// A `transform` function or a sequence of them, applied with [Styled#transform(Transform)].
/// Build values with the static factories and [#then(Transform)].
public sealed interface Transform extends CssValue
    permits Transforms.Scale, Transforms.Rotate, Transforms.TranslateY, Transforms.Chain {

  /// `scale(factor)`.
  static Transform scale(double factor) {
    return new Transforms.Scale(factor);
  }

  /// `rotate(<degrees>deg)`.
  static Transform rotate(double degrees) {
    return new Transforms.Rotate(degrees);
  }

  /// `translateY(distance)`.
  static Transform translateY(CssLength distance) {
    return new Transforms.TranslateY(distance);
  }

  /// A chain of this transform followed by `next`, written in that order in the CSS list. CSS
  /// composes them right to left, so the last function acts first on the element.
  default Transform then(Transform next) {
    var steps = new ArrayList<Transform>();
    steps.addAll(this instanceof Transforms.Chain chain ? chain.steps() : List.of(this));
    steps.addAll(next instanceof Transforms.Chain chain ? chain.steps() : List.of(next));
    return new Transforms.Chain(steps);
  }
}
