package dev.nthings.tessera.core;

import java.util.Locale;

/// Author-defined element name, such as `my-widget`, usually created with
/// [Elements#custom(String)]. The constructor requires a name that contains a hyphen and is
/// lowercase, and throws IllegalArgumentException otherwise. Other HTML name rules are not checked,
/// so a name the HTML standard reserves, such as `font-face`, is not rejected. Register the
/// element's module with [DocumentContext#contributeScript] so the browser upgrades it.
public record CustomElement(String tagName) implements TagName {

  public CustomElement {
    if (!tagName.contains("-") || !tagName.equals(tagName.toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException(
          "Custom element name must be lowercase and contain a hyphen: " + tagName);
    }
  }

  @Override
  public boolean isVoid() {
    return false;
  }
}
