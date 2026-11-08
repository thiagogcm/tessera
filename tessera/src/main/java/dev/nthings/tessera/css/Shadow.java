package dev.nthings.tessera.css;

import org.jspecify.annotations.Nullable;

/// One `box-shadow` layer.
public record Shadow(
    boolean inset,
    CssLength offsetX,
    CssLength offsetY,
    CssLength blur,
    @Nullable CssLength spread,
    CssColor color)
    implements CssValue {

  public static Shadow drop(CssLength offsetY, CssLength blur, CssColor color) {
    return new Shadow(false, CssLength.zero(), offsetY, blur, null, color);
  }

  /// Vertical offset that also grows or shrinks by `spread`.
  public static Shadow drop(CssLength offsetY, CssLength blur, CssLength spread, CssColor color) {
    return new Shadow(false, CssLength.zero(), offsetY, blur, spread, color);
  }

  @Override
  public String toCss() {
    var css = new StringBuilder();
    if (inset) {
      css.append("inset ");
    }
    css.append(offsetX.toCss())
        .append(' ')
        .append(offsetY.toCss())
        .append(' ')
        .append(blur.toCss());
    if (spread != null) {
      css.append(' ').append(spread.toCss());
    }
    return css.append(' ').append(color.toCss()).toString();
  }
}
