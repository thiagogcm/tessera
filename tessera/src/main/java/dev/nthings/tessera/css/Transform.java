package dev.nthings.tessera.css;

import com.google.errorprone.annotations.CheckReturnValue;
import java.util.ArrayList;
import java.util.List;

/// A `transform` function or a sequence of them, applied with [Styled#transform(Transform)].
/// Build values with the static factories and [#then(Transform)].
@CheckReturnValue
public sealed interface Transform extends CssValue
    permits Transforms.Scale,
        Transforms.ScaleX,
        Transforms.Rotate,
        Transforms.TranslateX,
        Transforms.TranslateY,
        Transforms.Chain {

  /// `scale(factor)`.
  static Transform scale(double factor) {
    return new Transforms.Scale(factor);
  }

  /// `scaleX(factor)`, scaling the horizontal axis only.
  static Transform scaleX(double factor) {
    return new Transforms.ScaleX(factor);
  }

  /// `rotate(<degrees>deg)`.
  static Transform rotate(double degrees) {
    return new Transforms.Rotate(degrees);
  }

  /// `translateX(distance)`, along the horizontal axis whatever the writing mode.
  static Transform translateX(CssLength distance) {
    return new Transforms.TranslateX(distance);
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
