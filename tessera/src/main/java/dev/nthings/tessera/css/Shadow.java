package dev.nthings.tessera.css;

import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/// One `box-shadow` layer, applied with [Styled#shadow]. Build it with [#drop] for a shadow cast
/// outside the box or [#inset] for one drawn inside it; both start with no horizontal offset, which
/// [#offsetX(CssLength)] sets. The value is immutable.
///
/// ```java
/// div().shadow(
///     Shadow.drop(CssLength.px(1), CssLength.px(2), Tokens.BORDER_SUBTLE),
///     Shadow.inset(CssLength.px(1), CssLength.zero(), Tokens.SURFACE_SUBTLE));
/// ```
@CheckReturnValue
public final class Shadow implements CssValue {

  private final boolean inset;
  private final CssLength offsetX;
  private final CssLength offsetY;
  private final CssLength blur;
  private final @Nullable CssLength spread;
  private final CssColor color;

  private Shadow(
      boolean inset,
      CssLength offsetX,
      CssLength offsetY,
      CssLength blur,
      @Nullable CssLength spread,
      CssColor color) {
    this.inset = inset;
    this.offsetX = offsetX;
    this.offsetY = offsetY;
    this.blur = blur;
    this.spread = spread;
    this.color = color;
  }

  /// A drop shadow without spread, as `0 <offsetY> <blur> <color>`.
  public static Shadow drop(CssLength offsetY, CssLength blur, CssColor color) {
    return new Shadow(false, CssLength.zero(), offsetY, blur, null, color);
  }

  /// A drop shadow that also grows or shrinks by `spread`, as `0 <offsetY> <blur> <spread>
  /// <color>`.
  public static Shadow drop(CssLength offsetY, CssLength blur, CssLength spread, CssColor color) {
    return new Shadow(false, CssLength.zero(), offsetY, blur, spread, color);
  }

  /// An inner shadow without spread, drawn inside the border box, as `inset 0 <offsetY> <blur>
  /// <color>`.
  public static Shadow inset(CssLength offsetY, CssLength blur, CssColor color) {
    return new Shadow(true, CssLength.zero(), offsetY, blur, null, color);
  }

  /// An inner shadow that also grows or shrinks by `spread`, as `inset 0 <offsetY> <blur>
  /// <spread> <color>`.
  public static Shadow inset(CssLength offsetY, CssLength blur, CssLength spread, CssColor color) {
    return new Shadow(true, CssLength.zero(), offsetY, blur, spread, color);
  }

  /// The same shadow moved horizontally by `offsetX`; positive values move it toward the right.
  public Shadow offsetX(CssLength offsetX) {
    return new Shadow(inset, offsetX, offsetY, blur, spread, color);
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

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof Shadow shadow
        && inset == shadow.inset
        && offsetX.equals(shadow.offsetX)
        && offsetY.equals(shadow.offsetY)
        && blur.equals(shadow.blur)
        && Objects.equals(spread, shadow.spread)
        && color.equals(shadow.color);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inset, offsetX, offsetY, blur, spread, color);
  }

  @Override
  public String toString() {
    return "Shadow(" + toCss() + ")";
  }
}
