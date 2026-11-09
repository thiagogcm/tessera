package dev.nthings.tessera.css;

import java.util.regex.Pattern;

/// Name rules and serialization shared by the [DesignToken] categories.
final class TokenNames {

  private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9_-]*");

  private TokenNames() {}

  /// Throws IllegalArgumentException unless `name` is `[A-Za-z0-9_-]+` without a leading `-`.
  static void require(String name) {
    if (!NAME.matcher(name).matches()) {
      throw new IllegalArgumentException(
          "A token name is [A-Za-z0-9_-]+ without a leading '-': '" + name + "'");
    }
  }

  /// `var(--name)`.
  static String varReference(String name) {
    return "var(--" + name + ")";
  }
}
