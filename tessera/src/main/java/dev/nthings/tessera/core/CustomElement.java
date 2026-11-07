package dev.nthings.tessera.core;

import java.util.Locale;

/// Author-defined custom element name.
///
/// Names are lowercase and contain a hyphen, so the browser upgrades them as autonomous custom
/// elements. Use [ClientBehavior] to register the module that upgrades the element.
public record CustomElement(String tagName) implements TagName {

  public CustomElement {
    if (tagName.isBlank()
        || !tagName.contains("-")
        || !tagName.equals(tagName.toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException(
          "Custom element name must be lowercase and contain a hyphen: " + tagName);
    }
  }

  @Override
  public boolean isVoid() {
    return false;
  }
}
