package dev.nthings.tessera.css;

/// An `aspect-ratio` of `width / height`.
public record AspectRatio(int width, int height) implements CssValue {

  public AspectRatio {
    if (width < 1 || height < 1) {
      throw new IllegalArgumentException("Aspect ratio terms must be positive");
    }
  }

  public static AspectRatio of(int width, int height) {
    return new AspectRatio(width, height);
  }

  public static AspectRatio square() {
    return new AspectRatio(1, 1);
  }

  @Override
  public String toCss() {
    return width + " / " + height;
  }
}
