package dev.nthings.tessera.css;

/// How replaced content such as an image fills its box, set with [Styled#objectFit(ObjectFit)].
public enum ObjectFit implements CssKeyword {
  FILL,
  CONTAIN,
  /// Fills the box and keeps the aspect ratio, cropping what overflows.
  COVER,
  NONE,
  SCALE_DOWN
}
