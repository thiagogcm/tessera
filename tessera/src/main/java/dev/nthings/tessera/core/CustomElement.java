package dev.nthings.tessera.core;

import java.util.Locale;

/// Author-defined custom element name: lowercase with a hyphen, so the browser upgrades it as an
/// autonomous custom element. Register its module with [ClientBehavior].
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
