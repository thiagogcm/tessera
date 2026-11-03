package dev.nthings.tessera.css;

import dev.nthings.tessera.core.Fnv1a64;

/**
 * Reactive CSS custom property token backed by native {@code light-dark()} or static CSS values.
 */
public record DesignToken(String name, String cssDefinition) {

  public DesignToken {
    Fnv1a64.sanitizeIdentifier(name);
  }

  public static DesignToken lightDark(String name, String lightValue, String darkValue) {
    return new DesignToken(
        Fnv1a64.sanitizeIdentifier(name), "light-dark(" + lightValue + ", " + darkValue + ")");
  }

  public static DesignToken value(String name, String rawValue) {
    return new DesignToken(Fnv1a64.sanitizeIdentifier(name), rawValue);
  }

  public String customPropertyName() {
    return "--" + name;
  }

  public String varReference() {
    return "var(--" + name + ")";
  }
}
