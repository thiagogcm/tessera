package dev.nthings.tessera.core;

import java.util.regex.Pattern;

/// Author-defined element name, such as `my-widget`, usually created with
/// [Elements#custom(String)]. The constructor requires a name that starts with a lowercase ASCII
/// letter, contains a hyphen, and uses only lowercase ASCII letters, digits, `.`, `_` and `-`, and
/// throws IllegalArgumentException otherwise. Other HTML name rules are not checked, so a name the
/// HTML standard reserves, such as `font-face`, is not rejected. Register the element's module from
/// a `DocumentComponent` with `DocumentContext#contributeScript(PageScript)` so a document upgrades
/// it.
public record CustomElement(String tagName) implements TagName {

  /// The ASCII subset of a valid custom element name: a lowercase letter, then lowercase letters,
  /// digits, `.`, `_` or `-`, with at least one `-`.
  private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9._-]*-[a-z0-9._-]*");

  public CustomElement {
    if (!NAME.matcher(tagName).matches()) {
      throw new IllegalArgumentException(
          "Custom element name must start with a lowercase ASCII letter, contain a hyphen, and"
              + " use only lowercase ASCII letters, digits, '.', '_' or '-': "
              + tagName);
    }
  }

  @Override
  public boolean isVoid() {
    return false;
  }
}
