package dev.nthings.tessera.css;

/// An `aspect-ratio` of `width / height`, set with [Styled#aspectRatio]. Throws
/// IllegalArgumentException unless both terms are positive.
public record AspectRatio(int width, int height) implements CssValue {

  public AspectRatio {
    if (width < 1 || height < 1) {
      throw new IllegalArgumentException("Aspect ratio terms must be positive");
    }
  }

  /// A ratio `width / height`, such as `16 / 9`.
  public static AspectRatio of(int width, int height) {
    return new AspectRatio(width, height);
  }

  /// `1 / 1`.
  public static AspectRatio square() {
    return new AspectRatio(1, 1);
  }

  @Override
  public String toCss() {
    return width + " / " + height;
  }
}
