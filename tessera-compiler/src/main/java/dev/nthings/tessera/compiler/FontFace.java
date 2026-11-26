package dev.nthings.tessera.compiler;

import com.google.errorprone.annotations.CheckReturnValue;
import java.util.List;
import java.util.stream.Collectors;
import org.owasp.encoder.Encode;

/// A self-hosted font file, registered on a site with [SiteDefinition.Builder#font(FontFace)]. The
/// site publishes the file, its stylesheet declares the `@font-face` rule with `font-display:
/// swap`, and with [#preload()] every page links the file with `<link rel="preload" as="font">`.
/// Point a font token at the family to use it, such as `theme.with(Tokens.FONT_BODY,
/// FontStack.of(FontStack.named("Inter"), FontStack.Generic.SYSTEM_UI))`.
///
/// ```java
/// SiteDefinition.builder()
///     .font(FontFace.of("Inter", Asset.file("/fonts/inter.woff2", Path.of("fonts/inter.woff2")))
///         .weight(100, 900)
///         .preload())
/// ```
///
/// @param family the `font-family` name the rule declares
/// @param file the font file, published at its path; its content type is the `format()` hint
/// @param weight the `font-weight` descriptor: one weight such as `400`, or a range `100 900` for a
///   variable font
/// @param style the `font-style` descriptor, `normal` or `italic`
/// @param preloaded whether every page preloads the file
@CheckReturnValue
public record FontFace(String family, Asset file, String weight, String style, boolean preloaded) {

  /// Throws IllegalArgumentException if `family` or `weight` is blank.
  public FontFace {
    if (family.isBlank()) {
      throw new IllegalArgumentException("Font family cannot be blank");
    }
    if (weight.isBlank()) {
      throw new IllegalArgumentException("Font weight cannot be blank");
    }
  }

  /// The upright, weight 400 face of `family` in `file`, not preloaded.
  public static FontFace of(String family, Asset file) {
    return new FontFace(family, file, "400", "normal", false);
  }

  /// The same face at `weight`, such as 600.
  public FontFace weight(int weight) {
    return new FontFace(family, file, Integer.toString(weight), style, preloaded);
  }

  /// The same face covering weights `min` to `max`, for a variable font.
  public FontFace weight(int min, int max) {
    return new FontFace(family, file, min + " " + max, style, preloaded);
  }

  /// The same face as the italic style.
  public FontFace italic() {
    return new FontFace(family, file, weight, "italic", preloaded);
  }

  /// The same face, preloaded by every page. Preload only the faces the first screen uses.
  public FontFace preload() {
    return new FontFace(family, file, weight, style, true);
  }

  /// The `@font-face` rules of `fonts`, in order.
  static String css(List<FontFace> fonts) {
    return fonts.stream().map(FontFace::rule).collect(Collectors.joining());
  }

  private String rule() {
    return "@font-face{font-family:\""
        + cssString(family)
        + "\";src:url(\""
        + cssString(file.path())
        + "\")"
        + format()
        + ";font-weight:"
        + weight
        + ";font-style:"
        + style
        + ";font-display:swap}";
  }

  /// `text` escaped for a double-quoted CSS string.
  private static String cssString(String text) {
    return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\a ");
  }

  private String format() {
    return switch (file.contentType()) {
      case "font/woff2" -> " format(\"woff2\")";
      case "font/woff" -> " format(\"woff\")";
      case "font/ttf" -> " format(\"truetype\")";
      case "font/otf" -> " format(\"opentype\")";
      default -> "";
    };
  }

  /// The `<link rel="preload">` of this face.
  String preloadLink() {
    return "<link rel=\"preload\" href=\""
        + Encode.forHtmlAttribute(file.path())
        + "\" as=\"font\" type=\""
        + Encode.forHtmlAttribute(file.contentType())
        + "\" crossorigin>";
  }
}
