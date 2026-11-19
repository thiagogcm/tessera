package dev.nthings.tessera.core;

/// Navigation and search metadata for one heading: the element bound to `target`, its plain label,
/// and its level. Expansion collects this from an element that opts into the outline; the first
/// heading for a target is kept. Throws IllegalArgumentException if `level` is outside 1 through 6.
public record OutlineHeading(ElementRef target, String title, int level) {
  public OutlineHeading {
    if (level < 1 || level > 6) {
      throw new IllegalArgumentException("Heading level must be between 1 and 6");
    }
  }
}
