package dev.nthings.tessera.support;

import java.util.Arrays;

/// Extracts rule blocks so tests can assert where a declaration lives, not match whole minified
/// CSS.
public final class CssBlocks {

  private CssBlocks() {}

  /// Body of the first rule whose selector is exactly `.className`.
  public static String classRule(String css, String className) {
    return block(css, "." + className + "{");
  }

  /// The generated class of `prefix` (`u-`, `i-`, `cq-`) in an element's `class` attribute.
  public static String generatedClass(String classAttr, String prefix) {
    return Arrays.stream(classAttr.split(" "))
        .filter(c -> c.startsWith(prefix))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No " + prefix + "* class in: " + classAttr));
  }

  /// Brace-balanced body after the first `opening` (which ends with `{`).
  public static String block(String css, String opening) {
    var start = css.indexOf(opening);
    if (start < 0) {
      throw new AssertionError("No block starting with " + opening);
    }
    var depth = 0;
    for (var i = start + opening.length() - 1; i < css.length(); i++) {
      var c = css.charAt(i);
      if (c == '{') {
        depth++;
      } else if (c == '}' && --depth == 0) {
        return css.substring(start + opening.length(), i);
      }
    }
    throw new AssertionError("Unbalanced block after " + opening);
  }
}
