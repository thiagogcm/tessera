package dev.nthings.tessera.core;

/// Navigation and search metadata for one heading on the page: the element bound to `target`, its
/// title and its level. Register one with [DocumentContext#registerHeading]; table-of-contents
/// components link to `target`. Throws IllegalArgumentException if `level` is outside 1 through 6.
public record DocumentHeading(ElementRef target, String title, int level) {
  public DocumentHeading {
    if (level < 1 || level > 6) {
      throw new IllegalArgumentException("Heading level must be between 1 and 6");
    }
  }
}
