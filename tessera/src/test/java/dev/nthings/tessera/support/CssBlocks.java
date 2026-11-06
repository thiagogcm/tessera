package dev.nthings.tessera.support;

import java.util.Arrays;

/// Extracts rule blocks from compiled stylesheets so tests can assert where a declaration lives
/// instead of matching whole minified strings.
public final class CssBlocks {

  private CssBlocks() {}

  /// The `{...}` body of the first rule whose prelude is exactly `.className`.
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

  /// The brace-balanced body following the first occurrence of `opening` (which ends with `{`).
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
